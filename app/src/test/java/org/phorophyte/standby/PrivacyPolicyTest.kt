package org.phorophyte.standby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The policy in the app and the one in the repo have to say the same thing. A privacy
 * policy that is accurate in one place and stale in the other is worse than not having
 * one, because both look authoritative.
 *
 * If this fails after an intentional edit, run `./gradlew testDebugUnitTest` and copy
 * `app/build/privacy/PRIVACY.md` over the one in the repo root.
 */
class PrivacyPolicyTest {

    /** Walks up from the module directory to wherever PRIVACY.md lives. */
    private fun repoFile(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "PRIVACY.md")
            if (candidate.exists()) return candidate
            dir = dir.parentFile
        }
        throw AssertionError("PRIVACY.md not found walking up from ${System.getProperty("user.dir")}")
    }

    /** Written on every run so regenerating after an edit is a copy, not a transcription. */
    @Test
    fun writeGeneratedCopyForComparison() {
        val out = File(File(System.getProperty("user.dir")!!), "build/privacy/PRIVACY.md")
        out.parentFile?.mkdirs()
        out.writeText(PrivacyPolicy.toMarkdown())
        assertTrue(out.exists())
    }

    @Test
    fun theRepoCopyMatchesTheAppCopy() {
        assertEquals(
            "PRIVACY.md is out of date. Copy app/build/privacy/PRIVACY.md over it.",
            PrivacyPolicy.toMarkdown(),
            repoFile().readText()
        )
    }

    /**
     * Every host the app can reach has to appear. Adding a network call without saying so
     * is the one failure mode that makes the whole document a lie.
     */
    @Test
    fun everyHostTheAppContactsIsListed() {
        val text = PrivacyPolicy.toMarkdown()
        for (host in listOf(
            "api.open-meteo.com",
            "geocoding-api.open-meteo.com",
            "ipapi.co",
            "openweathermap.org",
        )) {
            assertTrue("$host is not mentioned in the privacy policy", text.contains(host))
        }
    }

    @Test
    fun everyDeclaredPermissionIsExplained() {
        val text = PrivacyPolicy.toMarkdown().lowercase()
        for (phrase in listOf(
            "internet", "vibrate", "approximate location", "nearby devices", "query all packages",
        )) {
            assertTrue("\"$phrase\" is not explained in the privacy policy", text.contains(phrase))
        }
    }

    @Test
    fun theTablesAreWellFormed() {
        PrivacyPolicy.SECTIONS
            .flatMap { it.blocks }
            .filterIsInstance<PrivacyPolicy.Block.Table>()
            .forEach { table ->
                assertTrue("a table has no headers", table.headers.isNotEmpty())
                table.rows.forEach { row ->
                    assertEquals(
                        "row ${row.firstOrNull()} does not match its header count",
                        table.headers.size,
                        row.size
                    )
                }
            }
    }
}
