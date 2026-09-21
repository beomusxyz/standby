package org.phorophyte.standby

data class PluginModel(
    val localId: String,
    val manifestId: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val size: String = "full", // size type
    val orientation: String = PluginOrientation.RESPONSIVE,
    val permissions: List<String>,
    val providers: List<String> = emptyList(),
    val networkWhitelist: List<String>,
    val minAppVersion: Int,
    val directoryPath: String?, // null if built-in
    val htmlContent: String,
    val customizations: Map<String, CustomizationOption> = emptyMap(),
    val isBuiltIn: Boolean = false
)

object PluginOrientation {
    const val RESPONSIVE = "responsive"
    const val LANDSCAPE = "landscape"
    const val PORTRAIT = "portrait"
    val supported = setOf(RESPONSIVE, LANDSCAPE, PORTRAIT)
}

data class CustomizationOption(
    val type: String,
    val default: String,
    val target: String?, // css or js
    val value: String? = null,
    /** Only meaningful for `type: "enum"`. The choices the settings UI offers. */
    val options: List<String> = emptyList()
)

data class PendingPluginImport(
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val size: String,
    val orientation: String = PluginOrientation.RESPONSIVE,
    val permissions: List<String>,
    val providers: List<String> = emptyList(),
    val networkWhitelist: List<String>,
    val minAppVersion: Int,
    val isZip: Boolean,
    val tempDir: java.io.File,
    val originalFileName: String
)
