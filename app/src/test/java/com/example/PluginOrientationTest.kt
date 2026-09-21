package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginOrientationTest {

    @Test
    fun responsivePluginsFollowTheViewport() {
        assertEquals(0f, fixedOrientationRotation(PluginOrientation.RESPONSIVE, true))
        assertEquals(0f, fixedOrientationRotation(PluginOrientation.RESPONSIVE, false))
    }

    @Test
    fun fixedPluginsRotateOnlyWhenTheViewportDoesNotMatch() {
        assertEquals(0f, fixedOrientationRotation(PluginOrientation.LANDSCAPE, true))
        assertEquals(90f, fixedOrientationRotation(PluginOrientation.LANDSCAPE, false))
        assertEquals(90f, fixedOrientationRotation(PluginOrientation.PORTRAIT, true))
        assertEquals(0f, fixedOrientationRotation(PluginOrientation.PORTRAIT, false))
    }
}
