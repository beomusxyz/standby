package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PluginServerTest {

    private fun newServer(): PluginServer {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return PluginServer(context) { _, _ -> }
    }

    @Test
    fun pinIsSixDigits() {
        repeat(50) {
            val pin = newServer().pin
            assertEquals("PIN should be six characters: $pin", 6, pin.length)
            assertTrue("PIN should be all digits: $pin", pin.all(Char::isDigit))
        }
    }

    @Test
    fun pinVariesBetweenInstances() {
        // Not a randomness test, just a guard against anyone hardcoding it.
        val pins = (1..50).map { newServer().pin }.toSet()
        assertTrue("Expected varying PINs, got ${pins.size} distinct", pins.size > 1)
    }

    @Test
    fun hostHeaderAcceptsIpLiterals() {
        val server = newServer()
        assertTrue(server.isHostAllowed("192.168.1.42:45821"))
        assertTrue(server.isHostAllowed("10.0.0.5:8080"))
        assertTrue(server.isHostAllowed("172.16.4.1:45821"))
        assertTrue(server.isHostAllowed("127.0.0.1:45821"))
        assertTrue(server.isHostAllowed("localhost:45821"))
        assertTrue(server.isHostAllowed("[fe80::1]:45821"))
    }

    @Test
    fun hostHeaderRejectsDnsNames() {
        val server = newServer()
        // The DNS rebinding shape: an attacker controlled name pointed at the LAN IP.
        assertFalse(server.isHostAllowed("rebind.example.com:45821"))
        assertFalse(server.isHostAllowed("beef.com:45821"))
        assertFalse(server.isHostAllowed("standby.local:45821"))
        assertFalse(server.isHostAllowed(null))
        assertFalse(server.isHostAllowed(""))
    }

    @Test
    fun hostHeaderRejectsMalformedAddresses() {
        val server = newServer()
        assertFalse(server.isHostAllowed("999.1.1.1:80"))
        assertFalse(server.isHostAllowed("1.2.3:80"))
        assertFalse(server.isHostAllowed("1.2.3.4.5:80"))
        assertFalse(server.isHostAllowed("1.2.3.:80"))
    }
}
