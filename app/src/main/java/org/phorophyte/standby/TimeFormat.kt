package org.phorophyte.standby

/**
 * Whether a clock is drawn as 12 or 24 hour, for the app and for plugins.
 *
 * "Follow the system" is the reason this exists as a shared piece rather than a format
 * string at each call site. Android's toggle is `Settings.System.TIME_12_24`, which is
 * not part of the locale, so a plugin asking JavaScript
 * `Intl.DateTimeFormat().resolvedOptions().hour12` gets the locale default and is wrong
 * for anyone who has changed it. Only the app can answer, which is why the answer
 * reaches plugins over the sensor bridge instead of being left to them.
 *
 * Deliberately free of Android types so the resolution rules can be tested directly.
 */
object TimeFormat {

    /** App setting values. */
    const val SYSTEM = "system"
    const val TWELVE = "12"
    const val TWENTY_FOUR = "24"

    /** What a plugin sets to defer to the app. Its default, so silence means "defer". */
    const val INHERIT = "inherit"

    /** The customization a plugin declares to override the app setting. */
    const val PLUGIN_KEY = "TIME_FORMAT"

    /** Every value the app setting accepts, in the order the settings UI shows them. */
    val APP_OPTIONS = listOf(SYSTEM, TWELVE, TWENTY_FOUR)

    /** Every value a plugin's override accepts. */
    val PLUGIN_OPTIONS = listOf(INHERIT, TWELVE, TWENTY_FOUR)

    /** The app setting, resolved against whatever the system is set to. */
    fun resolveApp(setting: String?, systemIs24Hour: Boolean): Boolean = when (setting) {
        TWELVE -> false
        TWENTY_FOUR -> true
        else -> systemIs24Hour
    }

    /**
     * A single plugin's answer. Anything the plugin does not recognise, `inherit`
     * included, falls through to the app setting, so a plugin that never declares the
     * option behaves like one that declares `inherit`.
     */
    fun resolvePlugin(
        pluginValue: String?,
        appSetting: String?,
        systemIs24Hour: Boolean,
    ): Boolean = when (pluginValue) {
        TWELVE -> false
        TWENTY_FOUR -> true
        else -> resolveApp(appSetting, systemIs24Hour)
    }

    /** `HH:mm` or `h:mm a`, for the SimpleDateFormat patterns the bridge hands out. */
    fun pattern(use24Hour: Boolean, withSeconds: Boolean = false): String = when {
        use24Hour && withSeconds -> "HH:mm:ss"
        use24Hour -> "HH:mm"
        withSeconds -> "h:mm:ss a"
        else -> "h:mm a"
    }
}
