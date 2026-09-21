package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Weather settings used to live in one file under one set of keys, with no way to tell a
 * city the user typed from one the app resolved. Splitting them means moving what is
 * already on people's phones, and getting that wrong loses a setting silently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WeatherPrefsMigrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun settings() =
        context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private fun device() =
        context.getSharedPreferences(SettingsRepository.DEVICE_PREFS_NAME, Context.MODE_PRIVATE)

    @Before
    fun clear() {
        settings().edit().clear().commit()
        device().edit().clear().commit()
    }

    private fun writeLegacy(useGps: Boolean) {
        settings().edit()
            .putString("weather_lat", "-35.3081")
            .putString("weather_lon", "149.1244")
            .putString("weather_city", "Canberra")
            .putBoolean("weather_use_gps", useGps)
            .putString("weather_cache", """{"latitude":-35.3}""")
            .putLong("weather_last_update", 1234L)
            .putBoolean("server_enabled", true)
            .commit()
    }

    @Test
    fun aManuallyChosenCitySurvivesAsTheUsersOwn() {
        writeLegacy(useGps = false)
        val repo = SettingsRepository(context)

        assertEquals(SettingsRepository.MODE_MANUAL, repo.locationMode.value)
        assertEquals("Canberra", repo.manualCity.value)
        assertEquals("Canberra", repo.weatherCity.value)
        assertEquals("-35.3081", repo.weatherLat.value)
    }

    /**
     * With location on, what was stored came from GPS or the IP lookup. Promoting that to
     * "the city they chose" would be inventing a choice they never made, and would then
     * back it up.
     */
    @Test
    fun aResolvedCityIsNotPromotedToAChoice() {
        writeLegacy(useGps = true)
        val repo = SettingsRepository(context)

        assertEquals(SettingsRepository.MODE_COARSE, repo.locationMode.value)
        assertTrue(repo.weatherUseGps.value)
        assertNull(settings().getString("weather_manual_city", null))
        assertEquals("Canberra", device().getString(SettingsRepository.KEY_RESOLVED_CITY, null))
        assertEquals("Canberra", repo.weatherCity.value)
    }

    @Test
    fun derivedDataLandsInTheFileThatIsNotBackedUp() {
        writeLegacy(useGps = false)
        SettingsRepository(context)

        assertEquals("""{"latitude":-35.3}""", device().getString(SettingsRepository.KEY_WEATHER_CACHE, null))
        assertEquals(1234L, device().getLong(SettingsRepository.KEY_WEATHER_LAST_UPDATE, 0L))
        assertTrue(device().getBoolean(SettingsRepository.KEY_SERVER_ENABLED, false))

        assertNull(settings().getString("weather_cache", null))
        assertFalse(settings().contains("server_enabled"))
    }

    @Test
    fun theOldKeysAreGoneAfterwards() {
        writeLegacy(useGps = true)
        SettingsRepository(context)

        for (key in listOf(
            "weather_lat", "weather_lon", "weather_city", "weather_use_gps",
            "weather_cache", "weather_last_update", "server_enabled",
        )) {
            assertFalse("legacy key $key should have been removed", settings().contains(key))
        }
    }

    /** Running it twice must not undo the first run's work. */
    @Test
    fun migratingIsSafeToRepeat() {
        writeLegacy(useGps = false)
        SettingsRepository(context)
        val repo = SettingsRepository(context)

        assertEquals("Canberra", repo.manualCity.value)
        assertEquals(1234L, device().getLong(SettingsRepository.KEY_WEATHER_LAST_UPDATE, 0L))
    }

    /** A fresh install has nothing to move and must not be given Berlin as a "choice". */
    @Test
    fun aFreshInstallIsLeftAlone() {
        val repo = SettingsRepository(context)

        assertEquals(SettingsRepository.MODE_MANUAL, repo.locationMode.value)
        assertNull(settings().getString("weather_manual_city", null))
        assertFalse(device().contains(SettingsRepository.KEY_WEATHER_CACHE))
    }

    /** Switching to location and back has to leave the typed city where it was. */
    @Test
    fun locationModeNoLongerEatsTheManualCity() {
        val repo = SettingsRepository(context)
        repo.setWeatherLocation("-35.3081", "149.1244", "Canberra")
        assertEquals("Canberra", repo.weatherCity.value)

        repo.setWeatherUseGps(true)
        device().edit().putString(SettingsRepository.KEY_RESOLVED_CITY, "Queanbeyan").commit()
        assertEquals("Queanbeyan", repo.weatherCity.value)

        repo.setWeatherUseGps(false)
        assertEquals("Canberra", repo.weatherCity.value)
        assertEquals("Canberra", repo.manualCity.value)
    }
}
