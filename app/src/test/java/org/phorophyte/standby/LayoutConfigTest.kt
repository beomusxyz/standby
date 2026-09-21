package org.phorophyte.standby

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertNull(entry.pluginLocalId)
    }

    @Test
    fun stacksBecomeSplitsUsingTheirTopItems() {
        layoutFile.parentFile?.mkdirs()
        layoutFile.writeText(
            """{"pages":[{"type":"stack","plugin_local_id":null,"left_local_id":null,"right_local_id":null,"page_id":"stack-page","left_stack":["clock","weather"],"right_stack":["calendar","stocks"]}]}"""
        )

        val entry = PluginManager.loadLayoutConfig(context).single()

        assertEquals("half", entry.type)
        assertEquals("clock", entry.leftLocalId)
        assertEquals("calendar", entry.rightLocalId)

        val saved = JSONObject(layoutFile.readText()).getJSONArray("pages").getJSONObject(0)
        assertEquals("half", saved.getString("type"))
        assertFalse(saved.has("left_stack"))
        assertFalse(saved.has("right_stack"))
    }
}
