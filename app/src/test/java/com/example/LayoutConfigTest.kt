package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LayoutConfigTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val layoutFile: File
        get() = File(PluginManager.getPluginsDir(context), "pages_layout.json")

    @Before
    @After
    fun clearLayout() {
        layoutFile.delete()
    }

    @Test
    fun oldSplitLayoutsStillLoad() {
        layoutFile.parentFile?.mkdirs()
        layoutFile.writeText(
            """{"pages":[{"type":"half","plugin_local_id":null,"left_local_id":"left","right_local_id":"right","page_id":"old"}]}"""
        )

        val entry = PluginManager.loadLayoutConfig(context).single()

        assertEquals("half", entry.type)
        assertEquals("left", entry.leftLocalId)
        assertEquals("right", entry.rightLocalId)
        assertNull(entry.leftStack)
        assertNull(entry.rightStack)
    }

    @Test
    fun stacksKeepTheirOrderWhenSavedAndLoaded() {
        val expected = PluginManager.LayoutEntry(
            type = "stack",
            pluginLocalId = null,
            leftLocalId = null,
            rightLocalId = null,
            pageId = "stack-page",
            leftStack = listOf("clock", "appwidget:42", "weather"),
            rightStack = listOf("calendar", "stocks"),
        )

        PluginManager.saveLayoutConfig(context, listOf(expected))

        assertEquals(expected, PluginManager.loadLayoutConfig(context).single())
        val saved = JSONObject(layoutFile.readText()).getJSONArray("pages").getJSONObject(0)
        assertTrue(saved.has("left_stack"))
        assertEquals("appwidget:42", saved.getJSONArray("left_stack").getString(1))
        assertFalse(saved.isNull("page_id"))
    }
}
