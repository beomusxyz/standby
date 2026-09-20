package com.example

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class PluginServer(
    private val context: Context,
    private val onPluginReceived: (java.io.File, String) -> Unit
) {
    private var server: MyNanoHttpd? = null
    var port: Int = 0
        private set
    // Generated from a CSPRNG. kotlin.random.Random is a fast, seedable,
    // predictable PRNG -- fine for shuffling a list, never for a value an
    // attacker is trying to guess.
    val pin: String = String.format(Locale.US, "%06d", SecureRandom().nextInt(PIN_SPACE))
    var ipAddress: String = "127.0.0.1"
        private set

    // Brute-force throttle. Widening the PIN to 10^6 values only buys time if
    // the attacker is also limited in how fast they can spend guesses.
    private val failedAttempts = AtomicInteger(0)

    @Volatile
    private var lockedOutUntil: Long = 0L

    init {
        ipAddress = getLocalIpAddress()
    }

    fun start(): Boolean {
        return try {
            val s = MyNanoHttpd(0)
            s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = s
            port = s.listeningPort
            Log.d("PluginServer", "Server started successfully on IP $ipAddress and port $port with PIN $pin")
            true
        } catch (e: Exception) {
            Log.e("PluginServer", "Failed to start server", e)
            false
        }
    }

    fun stop() {
        try {
            server?.stop()
            server = null
            Log.d("PluginServer", "Server stopped")
        } catch (e: Exception) {
            Log.e("PluginServer", "Failed to stop server", e)
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress ?: continue
                        // return first lan ip
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PluginServer", "Error getting IP address", e)
        }
        return "127.0.0.1"
    }

    private fun getUploaderHtml(): String {
        return try {
            context.assets.open("uploader.html").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e("PluginServer", "Error reading uploader.html from assets", e)
            "Error: Could not load uploader.html"
        }
    }

    /**
     * Rejects any Host header that is not an IP literal.
     *
     * Without this, DNS rebinding walks straight past the same-origin policy.
     * The attacker points a name they control at their own server, you load
     * their page, then they repoint the name at this phone's LAN address. The
     * browser still believes it is talking to evil.example, so their script is
     * same-origin with this server and can read every response. Legitimate use
     * always arrives via the bare IP printed in the UI, so a name in the Host
     * header means something is off.
     */
    internal fun isHostAllowed(hostHeader: String?): Boolean {
        if (hostHeader.isNullOrBlank()) return false
        // Bracketed IPv6 literal, e.g. [fe80::1]:45821
        if (hostHeader.startsWith("[")) return true
        val host = hostHeader.substringBefore(':')
        return host.equals("localhost", ignoreCase = true) || isIpv4Literal(host)
    }

    private fun isIpv4Literal(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 &&
                part.all(Char::isDigit) && part.toInt() in 0..255
        }
    }

    /**
     * Compares in time that does not depend on how many leading characters
     * match. String's own != bails out at the first difference, so how long a
     * rejection takes leaks how much of the guess was right.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    private fun registerFailedAttempt() {
        if (failedAttempts.incrementAndGet() >= MAX_FAILED_ATTEMPTS) {
            // Reset the counter as we lock out, so that when the window expires
            // the client gets a fresh allowance instead of re-locking instantly.
            failedAttempts.set(0)
            lockedOutUntil = System.currentTimeMillis() + LOCKOUT_MS
            Log.w("PluginServer", "Too many incorrect PINs; uploads locked for ${LOCKOUT_MS / 1000}s")
        }
    }

    private inner class MyNanoHttpd(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

            // No CORS headers, deliberately. The uploader page is served by this
            // same server and is therefore already same-origin, so it needs none.
            // Their absence is what stops any other website scripting an upload:
            // both the X-PIN header and the application/zip content type force
            // the browser to send a preflight first, and a preflight that comes
            // back without Allow-Origin fails, so the real request never leaves.
            fun createResponse(status: Response.IStatus, mimeType: String, message: String): Response =
                newFixedLengthResponse(status, mimeType, message)

            // NanoHTTPD lowercases header names as it parses them, so "host" is
            // the correct lookup whatever casing the client actually sent.
            if (!isHostAllowed(session.headers["host"])) {
                Log.w("PluginServer", "Rejected request with non-literal Host header")
                return createResponse(Response.Status.FORBIDDEN, "text/plain; charset=utf-8", "Forbidden.")
            }

            if ("/" == uri && Method.GET == method) {
                return createResponse(Response.Status.OK, "text/html; charset=utf-8", getUploaderHtml())
            }

            if ("/upload" == uri && Method.POST == method) {
                val now = System.currentTimeMillis()
                if (now < lockedOutUntil) {
                    val waitSeconds = (lockedOutUntil - now) / 1000 + 1
                    return createResponse(
                        Response.Status.TOO_MANY_REQUESTS,
                        "text/plain; charset=utf-8",
                        "Too many incorrect PINs. Try again in ${waitSeconds}s."
                    )
                }

                val requestPin = session.headers["x-pin"] ?: session.headers["X-PIN"]
                if (requestPin == null || !constantTimeEquals(requestPin, pin)) {
                    registerFailedAttempt()
                    return createResponse(Response.Status.UNAUTHORIZED, "text/plain; charset=utf-8", "Incorrect PIN/Passcode. Please try again.")
                }
                failedAttempts.set(0)

                return try {
                    val contentLengthStr = session.headers["content-length"] ?: session.headers["Content-Length"]
                    val contentLength = contentLengthStr?.toIntOrNull() ?: 0
                    val contentType = session.headers["content-type"] ?: session.headers["Content-Type"] ?: "text/html"

                    if (contentLength <= 0) {
                        createResponse(Response.Status.BAD_REQUEST, "text/plain; charset=utf-8", "Empty upload content.")
                    } else {
                        // create temp file
                        val tempFile = java.io.File.createTempFile("upload_", ".tmp", context.cacheDir)
                        tempFile.outputStream().use { output ->
                            val input = session.inputStream
                            val buffer = ByteArray(8192)
                            var bytesRemaining = contentLength
                            while (bytesRemaining > 0) {
                                val toRead = Math.min(buffer.size, bytesRemaining)
                                val read = input.read(buffer, 0, toRead)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                bytesRemaining -= read
                            }
                        }

                        onPluginReceived(tempFile, contentType)
                        
                        val prefs = context.getSharedPreferences("standby_settings", Context.MODE_PRIVATE)
                        val confirmEnabled = prefs.getBoolean("confirm_plugin_import", true)
                        val responseMsg = if (confirmEnabled) {
                            "Upload received!"
                        } else {
                            "Success! Plugin updated."
                        }
                        createResponse(Response.Status.OK, "text/plain; charset=utf-8", responseMsg)
                    }
                } catch (e: Exception) {
                    Log.e("PluginServer", "Error handling upload", e)
                    createResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=utf-8", "Server error occurred.")
                }
            }

            return createResponse(Response.Status.NOT_FOUND, "text/plain; charset=utf-8", "Not Found")
        }
    }

    companion object {
        /** 10^6, i.e. a six digit PIN. */
        private const val PIN_SPACE = 1_000_000

        /** Consecutive wrong PINs tolerated before uploads are locked out. */
        private const val MAX_FAILED_ATTEMPTS = 5

        private const val LOCKOUT_MS = 60_000L
    }
}
