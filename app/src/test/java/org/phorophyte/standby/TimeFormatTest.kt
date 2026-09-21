package org.phorophyte.standby

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The resolution rules, which is the part worth pinning. Everything else about the time
 * format is UI or a bridge call.
 */
class TimeFormatTest {

    @Test
    fun anExplicitAppSettingIgnoresTheSystem() {
        assertFalse(TimeFormat.resolveApp(TimeFormat.TWELVE, systemIs24Hour = true))
        assertTrue(TimeFormat.resolveApp(TimeFormat.TWENTY_FOUR, systemIs24Hour = false))
    }

    @Test
    fun systemFollowsTheSystem() {
        assertTrue(TimeFormat.resolveApp(TimeFormat.SYSTEM, systemIs24Hour = true))
        assertFalse(TimeFormat.resolveApp(TimeFormat.SYSTEM, systemIs24Hour = false))
    }

    /** A value we do not recognise must not silently mean 12 hour. */
    @Test
    fun anythingUnrecognisedFallsBackToTheSystem() {
        assertTrue(TimeFormat.resolveApp(null, systemIs24Hour = true))
        assertTrue(TimeFormat.resolveApp("", systemIs24Hour = true))
        assertTrue(TimeFormat.resolveApp("banana", systemIs24Hour = true))
    }

    @Test
    fun aPluginOverridesTheApp() {
        assertFalse(
            TimeFormat.resolvePlugin(
                pluginValue = TimeFormat.TWELVE,
                appSetting = TimeFormat.TWENTY_FOUR,
                systemIs24Hour = true,
            )
        )
        assertTrue(
            TimeFormat.resolvePlugin(
                pluginValue = TimeFormat.TWENTY_FOUR,
                appSetting = TimeFormat.TWELVE,
                systemIs24Hour = false,
            )
        )
    }

    /**
     * The case that matters most: a plugin that never declares the option has to behave
     * exactly like one that declares `inherit`, or every existing plugin breaks.
     */
    @Test
    fun aSilentPluginDefersToTheApp() {
        for (quiet in listOf(null, "", TimeFormat.INHERIT)) {
            assertTrue(
                "plugin value <$quiet> should defer to the app",
                TimeFormat.resolvePlugin(quiet, TimeFormat.TWENTY_FOUR, systemIs24Hour = false)
            )
            assertFalse(
                "plugin value <$quiet> should defer to the app",
                TimeFormat.resolvePlugin(quiet, TimeFormat.TWELVE, systemIs24Hour = true)
            )
        }
    }

    @Test
    fun inheritChainsAllTheWayToTheSystem() {
        assertTrue(
            TimeFormat.resolvePlugin(TimeFormat.INHERIT, TimeFormat.SYSTEM, systemIs24Hour = true)
        )
        assertFalse(
            TimeFormat.resolvePlugin(TimeFormat.INHERIT, TimeFormat.SYSTEM, systemIs24Hour = false)
        )
    }

    @Test
    fun patternsMatchTheResolvedFormat() {
        assertTrue(TimeFormat.pattern(use24Hour = true) == "HH:mm")
        assertTrue(TimeFormat.pattern(use24Hour = false) == "h:mm a")
        assertTrue(TimeFormat.pattern(use24Hour = true, withSeconds = true) == "HH:mm:ss")
    }
}
