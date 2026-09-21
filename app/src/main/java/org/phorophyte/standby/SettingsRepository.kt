package org.phorophyte.standby

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the settings file and is the one place settings are read from.
 *
 * Before this, each setting was a MutableStateFlow seeded from SharedPreferences in
 * StandbyViewModel's constructor, read once and never refreshed. That holds up while
 * there is one ViewModel. With two, which is what adding a DreamService gives you, a
 * write from one instance reaches disk and never reaches the other instance's flow. The
 * two drift apart until the process restarts, with nothing in logcat to say so.
 *
 * Writes here go to SharedPreferences, and the change listener puts the value back into
 * the flow. Readers agree because they all take the same route. One per process.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Everything this device worked out for itself, kept apart from what the user chose.
     *
     * The split is for backup. Android's backup rules exclude SharedPreferences a whole
     * file at a time, never per key, so "back up their settings but not their
     * coordinates" is only expressible as two files. Excluded from backup in
     * res/xml/backup_rules.xml and res/xml/data_extraction_rules.xml.
     */
    private val devicePrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(DEVICE_PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Per-plugin values keyed by plugin id, like `builtin_name_<id>`, have no fixed key
     * set and are not app settings, so [DefaultPlugins] still reads them straight from
     * prefs. Exposed `internal` rather than pretending this class owns every key in the
     * file.
     */
    internal val preferences: SharedPreferences get() = prefs

    /** How to re-read each key, populated as the settings below are declared. */
    private val reloaders = mutableMapOf<String, () -> Unit>()

    /**
     * Held in a field deliberately. SharedPreferences keeps listeners in a WeakHashMap, so
     * one that exists only as a local gets collected and stops firing. The symptom is
     * settings that update for a while and then quietly stop.
     */
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        key?.let { reloaders[it]?.invoke() }
    }

    init {
        migrateLegacyWeatherKeys()
        prefs.registerOnSharedPreferenceChangeListener(listener)
        devicePrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Before the split, one file held both. Coordinates the app had resolved sat next to
     * the city the user typed, under the same keys, so there was no way to tell them
     * apart and turning location on overwrote a manually chosen city for good.
     *
     * Runs once: the old keys are removed as they are moved.
     */
    private fun migrateLegacyWeatherKeys() {
        if (!prefs.contains(LEGACY_KEY_LAT) &&
            !prefs.contains(LEGACY_KEY_CITY) &&
            !prefs.contains(LEGACY_KEY_CACHE) &&
            !prefs.contains(LEGACY_KEY_USE_GPS) &&
            !prefs.contains(LEGACY_KEY_SERVER_ENABLED)
        ) return

        val wasUsingGps = prefs.getBoolean(LEGACY_KEY_USE_GPS, false)
        val lat = prefs.getString(LEGACY_KEY_LAT, null)
        val lon = prefs.getString(LEGACY_KEY_LON, null)
        val city = prefs.getString(LEGACY_KEY_CITY, null)

        devicePrefs.edit().apply {
            // Whatever was on disk describes this device either way, so it lands here.
            lat?.let { putString(KEY_RESOLVED_LAT, it) }
            lon?.let { putString(KEY_RESOLVED_LON, it) }
            city?.let { putString(KEY_RESOLVED_CITY, it) }
            prefs.getString(LEGACY_KEY_CACHE, null)?.let { putString(KEY_WEATHER_CACHE, it) }
            putLong(KEY_WEATHER_LAST_UPDATE, prefs.getLong(LEGACY_KEY_LAST_UPDATE, 0L))
            putBoolean(KEY_SERVER_ENABLED, prefs.getBoolean(LEGACY_KEY_SERVER_ENABLED, false))
        }.apply()

        prefs.edit().apply {
            putString(KEY_LOCATION_MODE, if (wasUsingGps) MODE_COARSE else MODE_MANUAL)
            // Only a manual setup can claim these as the user's choice. If location was
            // on, what was stored came from GPS or the IP lookup, and it stays device-side.
            if (!wasUsingGps) {
                lat?.let { putString(KEY_MANUAL_LAT, it) }
                lon?.let { putString(KEY_MANUAL_LON, it) }
                city?.let { putString(KEY_MANUAL_CITY, it) }
            }
            remove(LEGACY_KEY_LAT)
            remove(LEGACY_KEY_LON)
            remove(LEGACY_KEY_CITY)
            remove(LEGACY_KEY_USE_GPS)
            remove(LEGACY_KEY_CACHE)
            remove(LEGACY_KEY_LAST_UPDATE)
            remove(LEGACY_KEY_SERVER_ENABLED)
        }.apply()
    }

    // --- flow builders -------------------------------------------------------------
    // Each seeds a flow from prefs and records how to re-read that key later.

    private fun boolFlow(key: String, default: Boolean): MutableStateFlow<Boolean> =
        MutableStateFlow(prefs.getBoolean(key, default)).also { flow ->
            reloaders[key] = { flow.value = prefs.getBoolean(key, default) }
        }

    private fun intFlow(key: String, default: Int): MutableStateFlow<Int> =
        MutableStateFlow(prefs.getInt(key, default)).also { flow ->
            reloaders[key] = { flow.value = prefs.getInt(key, default) }
        }

    private fun floatFlow(key: String, default: Float): MutableStateFlow<Float> =
        MutableStateFlow(prefs.getFloat(key, default)).also { flow ->
            reloaders[key] = { flow.value = prefs.getFloat(key, default) }
        }

    private fun longFlow(key: String, default: Long): MutableStateFlow<Long> =
        MutableStateFlow(prefs.getLong(key, default)).also { flow ->
            reloaders[key] = { flow.value = prefs.getLong(key, default) }
        }

    private fun stringFlow(key: String, default: String): MutableStateFlow<String> =
        MutableStateFlow(prefs.getString(key, default) ?: default).also { flow ->
            reloaders[key] = { flow.value = prefs.getString(key, default) ?: default }
        }

    private fun deviceStringFlow(key: String, default: String): MutableStateFlow<String> =
        MutableStateFlow(devicePrefs.getString(key, default) ?: default).also { flow ->
            reloaders[key] = { flow.value = devicePrefs.getString(key, default) ?: default }
        }

    private fun deviceBoolFlow(key: String, default: Boolean): MutableStateFlow<Boolean> =
        MutableStateFlow(devicePrefs.getBoolean(key, default)).also { flow ->
            reloaders[key] = { flow.value = devicePrefs.getBoolean(key, default) }
        }

    private fun deviceLongFlow(key: String, default: Long): MutableStateFlow<Long> =
        MutableStateFlow(devicePrefs.getLong(key, default)).also { flow ->
            reloaders[key] = { flow.value = devicePrefs.getLong(key, default) }
        }

    // --- burn-in protection and display --------------------------------------------

    private val _burnInProtectionEnabled = boolFlow(KEY_BURN_IN_PROTECTION, true)
    val burnInProtectionEnabled: StateFlow<Boolean> = _burnInProtectionEnabled.asStateFlow()

    private val _delayAfterInteraction = boolFlow(KEY_DELAY_AFTER_INTERACTION, false)
    val delayAfterInteraction: StateFlow<Boolean> = _delayAfterInteraction.asStateFlow()

    private val _protectionRatio = intFlow(KEY_PROTECTION_RATIO, 1)
    val protectionRatio: StateFlow<Int> = _protectionRatio.asStateFlow()

    private val _hideControlsOnIdle = boolFlow(KEY_HIDE_CONTROLS_ON_IDLE, true)
    val hideControlsOnIdle: StateFlow<Boolean> = _hideControlsOnIdle.asStateFlow()

    private val _lowRefreshRateEnabled = boolFlow(KEY_LOW_REFRESH_RATE_ENABLED, false)
    val lowRefreshRateEnabled: StateFlow<Boolean> = _lowRefreshRateEnabled.asStateFlow()

    private val _lowRefreshRateValue = intFlow(KEY_LOW_REFRESH_RATE_VALUE, 60)
    val lowRefreshRateValue: StateFlow<Int> = _lowRefreshRateValue.asStateFlow()

    /**
     * `system`, `12` or `24`. Defaults to following the system, which is the only one of
     * the three the app cannot work out from the locale. See [TimeFormat].
     */
    private val _timeFormat = stringFlow(KEY_TIME_FORMAT, TimeFormat.SYSTEM)
    val timeFormat: StateFlow<String> = _timeFormat.asStateFlow()

    fun setTimeFormat(value: String) = putString(KEY_TIME_FORMAT, value)

    fun setBurnInProtectionEnabled(enabled: Boolean) = putBoolean(KEY_BURN_IN_PROTECTION, enabled)
    fun setDelayAfterInteraction(enabled: Boolean) = putBoolean(KEY_DELAY_AFTER_INTERACTION, enabled)
    fun setProtectionRatio(ratio: Int) = putInt(KEY_PROTECTION_RATIO, ratio)
    fun setHideControlsOnIdle(enabled: Boolean) = putBoolean(KEY_HIDE_CONTROLS_ON_IDLE, enabled)
    fun setLowRefreshRateEnabled(enabled: Boolean) = putBoolean(KEY_LOW_REFRESH_RATE_ENABLED, enabled)
    fun setLowRefreshRateValue(value: Int) = putInt(KEY_LOW_REFRESH_RATE_VALUE, value)

    // --- night mode ------------------------------------------------------------------

    private val _nightModeEnabled = boolFlow(KEY_NIGHT_MODE_ENABLED, false)
    val nightModeEnabled: StateFlow<Boolean> = _nightModeEnabled.asStateFlow()

    private val _nightStartHour = intFlow(KEY_NIGHT_START_HOUR, 22)
    val nightStartHour: StateFlow<Int> = _nightStartHour.asStateFlow()

    private val _nightStartMinute = intFlow(KEY_NIGHT_START_MINUTE, 0)
    val nightStartMinute: StateFlow<Int> = _nightStartMinute.asStateFlow()

    private val _nightEndHour = intFlow(KEY_NIGHT_END_HOUR, 7)
    val nightEndHour: StateFlow<Int> = _nightEndHour.asStateFlow()

    private val _nightEndMinute = intFlow(KEY_NIGHT_END_MINUTE, 0)
    val nightEndMinute: StateFlow<Int> = _nightEndMinute.asStateFlow()

    private val _nightProtectionRatio = intFlow(KEY_NIGHT_PROTECTION_RATIO, 4)
    val nightProtectionRatio: StateFlow<Int> = _nightProtectionRatio.asStateFlow()

    private val _nightBrightnessEnabled = boolFlow(KEY_NIGHT_BRIGHTNESS_ENABLED, true)
    val nightBrightnessEnabled: StateFlow<Boolean> = _nightBrightnessEnabled.asStateFlow()

    private val _nightBrightnessValue = floatFlow(KEY_NIGHT_BRIGHTNESS_VALUE, 0.05f)
    val nightBrightnessValue: StateFlow<Float> = _nightBrightnessValue.asStateFlow()

    fun setNightModeEnabled(enabled: Boolean) = putBoolean(KEY_NIGHT_MODE_ENABLED, enabled)
    fun setNightProtectionRatio(ratio: Int) = putInt(KEY_NIGHT_PROTECTION_RATIO, ratio)
    fun setNightBrightnessEnabled(enabled: Boolean) = putBoolean(KEY_NIGHT_BRIGHTNESS_ENABLED, enabled)
    fun setNightBrightnessValue(value: Float) = putFloat(KEY_NIGHT_BRIGHTNESS_VALUE, value)

    fun setNightStartTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_NIGHT_START_HOUR, hour)
            .putInt(KEY_NIGHT_START_MINUTE, minute)
            .apply()
    }

    fun setNightEndTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_NIGHT_END_HOUR, hour)
            .putInt(KEY_NIGHT_END_MINUTE, minute)
            .apply()
    }

    // --- weather -----------------------------------------------------------------------

    /** [MODE_MANUAL] or [MODE_COARSE]. */
    private val _locationMode = stringFlow(KEY_LOCATION_MODE, MODE_MANUAL)
    val locationMode: StateFlow<String> = _locationMode.asStateFlow()

    // What the user picked. Survives switching to location mode and back, which the
    // single-key version did not.
    private val _manualLat = stringFlow(KEY_MANUAL_LAT, DEFAULT_LAT)
    private val _manualLon = stringFlow(KEY_MANUAL_LON, DEFAULT_LON)
    private val _manualCity = stringFlow(KEY_MANUAL_CITY, DEFAULT_CITY)
    val manualCity: StateFlow<String> = _manualCity.asStateFlow()

    // What this device resolved, from GPS or the IP lookup.
    private val _resolvedLat = deviceStringFlow(KEY_RESOLVED_LAT, DEFAULT_LAT)
    private val _resolvedLon = deviceStringFlow(KEY_RESOLVED_LON, DEFAULT_LON)
    private val _resolvedCity = deviceStringFlow(KEY_RESOLVED_CITY, DEFAULT_CITY)

    private val _weatherLastUpdate = deviceLongFlow(KEY_WEATHER_LAST_UPDATE, 0L)
    val weatherLastUpdate: StateFlow<Long> = _weatherLastUpdate.asStateFlow()

    // The effective values, which is what the UI wants. Recomputed by the chained
    // reloaders in the init block below rather than a combine(), which would need a scope.
    private val _weatherLat = MutableStateFlow(effectiveLat())
    val weatherLat: StateFlow<String> = _weatherLat.asStateFlow()

    private val _weatherLon = MutableStateFlow(effectiveLon())
    val weatherLon: StateFlow<String> = _weatherLon.asStateFlow()

    private val _weatherCity = MutableStateFlow(effectiveCity())
    val weatherCity: StateFlow<String> = _weatherCity.asStateFlow()

    /** Kept as a boolean because that is what the settings switch is. */
    val weatherUseGps: StateFlow<Boolean> = MutableStateFlow(usingCoarse()).also { flow ->
        val previous = reloaders[KEY_LOCATION_MODE]
        reloaders[KEY_LOCATION_MODE] = {
            previous?.invoke()
            flow.value = usingCoarse()
        }
    }.asStateFlow()

    init {
        // Any of these changing moves the effective values, so chain onto whatever the
        // builders already registered rather than replacing it.
        listOf(
            KEY_LOCATION_MODE, KEY_MANUAL_LAT, KEY_MANUAL_LON, KEY_MANUAL_CITY,
            KEY_RESOLVED_LAT, KEY_RESOLVED_LON, KEY_RESOLVED_CITY,
        ).forEach { key ->
            val previous = reloaders[key]
            reloaders[key] = {
                previous?.invoke()
                _weatherLat.value = effectiveLat()
                _weatherLon.value = effectiveLon()
                _weatherCity.value = effectiveCity()
            }
        }
    }

    private fun usingCoarse() = prefs.getString(KEY_LOCATION_MODE, MODE_MANUAL) == MODE_COARSE

    private fun effectiveLat() =
        if (usingCoarse()) devicePrefs.getString(KEY_RESOLVED_LAT, DEFAULT_LAT) ?: DEFAULT_LAT
        else prefs.getString(KEY_MANUAL_LAT, DEFAULT_LAT) ?: DEFAULT_LAT

    private fun effectiveLon() =
        if (usingCoarse()) devicePrefs.getString(KEY_RESOLVED_LON, DEFAULT_LON) ?: DEFAULT_LON
        else prefs.getString(KEY_MANUAL_LON, DEFAULT_LON) ?: DEFAULT_LON

    private fun effectiveCity() =
        if (usingCoarse()) devicePrefs.getString(KEY_RESOLVED_CITY, DEFAULT_CITY) ?: DEFAULT_CITY
        else prefs.getString(KEY_MANUAL_CITY, DEFAULT_CITY) ?: DEFAULT_CITY

    fun setWeatherUseGps(enabled: Boolean) =
        putString(KEY_LOCATION_MODE, if (enabled) MODE_COARSE else MODE_MANUAL)

    /** Picking a city is a manual choice, so it sets the mode as well as the values. */
    fun setWeatherLocation(lat: String, lon: String, city: String) {
        prefs.edit()
            .putString(KEY_MANUAL_LAT, lat)
            .putString(KEY_MANUAL_LON, lon)
            .putString(KEY_MANUAL_CITY, city)
            .putString(KEY_LOCATION_MODE, MODE_MANUAL)
            .apply()
    }

    // --- plugins and the upload server --------------------------------------------------

    private val _confirmImportEnabled = boolFlow(KEY_CONFIRM_PLUGIN_IMPORT, true)
    val confirmImportEnabled: StateFlow<Boolean> = _confirmImportEnabled.asStateFlow()

    private val _appWidgetsEnabled = boolFlow(KEY_APP_WIDGETS_ENABLED, true)
    val appWidgetsEnabled: StateFlow<Boolean> = _appWidgetsEnabled.asStateFlow()

    /** Off by default. An upload server nobody is using is a listening socket nobody asked for. */
    private val _serverEnabled = deviceBoolFlow(KEY_SERVER_ENABLED, false)
    val serverEnabled: StateFlow<Boolean> = _serverEnabled.asStateFlow()

    fun setConfirmImportEnabled(enabled: Boolean) = putBoolean(KEY_CONFIRM_PLUGIN_IMPORT, enabled)
    fun setAppWidgetsEnabled(enabled: Boolean) = putBoolean(KEY_APP_WIDGETS_ENABLED, enabled)
    /** Device-side, so a restored phone does not come up already listening. */
    fun setServerEnabled(enabled: Boolean) =
        devicePrefs.edit().putBoolean(KEY_SERVER_ENABLED, enabled).apply()

    // --- writes ---------------------------------------------------------------------------
    // Writes go to prefs and nowhere else. The change listener puts the value back into the
    // flow, so the writer arrives at the new value by the same route as everyone else.

    private fun putBoolean(key: String, value: Boolean) =
        prefs.edit().putBoolean(key, value).apply()

    private fun putInt(key: String, value: Int) =
        prefs.edit().putInt(key, value).apply()

    private fun putFloat(key: String, value: Float) =
        prefs.edit().putFloat(key, value).apply()

    private fun putString(key: String, value: String) =
        prefs.edit().putString(key, value).apply()

    companion object {
        const val PREFS_NAME = "standby_settings"
        const val DEVICE_PREFS_NAME = "standby_device"

        const val DEFAULT_LAT = "52.52"
        const val DEFAULT_LON = "13.41"
        const val DEFAULT_CITY = "Berlin"

        private const val KEY_BURN_IN_PROTECTION = "burn_in_protection"
        private const val KEY_DELAY_AFTER_INTERACTION = "delay_after_interaction"
        private const val KEY_PROTECTION_RATIO = "protection_ratio"
        private const val KEY_HIDE_CONTROLS_ON_IDLE = "hide_controls_on_idle"
        private const val KEY_LOW_REFRESH_RATE_ENABLED = "low_refresh_rate_enabled"
        private const val KEY_LOW_REFRESH_RATE_VALUE = "low_refresh_rate_value"
        private const val KEY_TIME_FORMAT = "time_format"

        private const val KEY_NIGHT_MODE_ENABLED = "night_mode_enabled"
        private const val KEY_NIGHT_START_HOUR = "night_mode_start_hour"
        private const val KEY_NIGHT_START_MINUTE = "night_mode_start_minute"
        private const val KEY_NIGHT_END_HOUR = "night_mode_end_hour"
        private const val KEY_NIGHT_END_MINUTE = "night_mode_end_minute"
        private const val KEY_NIGHT_PROTECTION_RATIO = "night_protection_ratio"
        private const val KEY_NIGHT_BRIGHTNESS_ENABLED = "night_brightness_enabled"
        private const val KEY_NIGHT_BRIGHTNESS_VALUE = "night_brightness_value"

        const val MODE_MANUAL = "manual"
        const val MODE_COARSE = "coarse"

        private const val KEY_LOCATION_MODE = "weather_location_mode"
        private const val KEY_MANUAL_LAT = "weather_manual_lat"
        private const val KEY_MANUAL_LON = "weather_manual_lon"
        private const val KEY_MANUAL_CITY = "weather_manual_city"

        // Read directly by ProviderManager and ProviderBridge, which own the fetching.
        const val KEY_RESOLVED_LAT = "weather_resolved_lat"
        const val KEY_RESOLVED_LON = "weather_resolved_lon"
        const val KEY_RESOLVED_CITY = "weather_resolved_city"
        const val KEY_WEATHER_CACHE = "weather_cache"
        const val KEY_WEATHER_LAST_UPDATE = "weather_last_update"

        // Only read once, by the migration.
        private const val LEGACY_KEY_LAT = "weather_lat"
        private const val LEGACY_KEY_LON = "weather_lon"
        private const val LEGACY_KEY_CITY = "weather_city"
        private const val LEGACY_KEY_USE_GPS = "weather_use_gps"
        private const val LEGACY_KEY_CACHE = "weather_cache"
        private const val LEGACY_KEY_LAST_UPDATE = "weather_last_update"
        private const val LEGACY_KEY_SERVER_ENABLED = "server_enabled"

        private const val KEY_CONFIRM_PLUGIN_IMPORT = "confirm_plugin_import"
        private const val KEY_APP_WIDGETS_ENABLED = "app_widgets_enabled"
        const val KEY_SERVER_ENABLED = "server_enabled"
    }
}
