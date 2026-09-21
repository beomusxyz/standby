package org.phorophyte.standby

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProviderManagerTest {

    @Test
    fun testSearchLocationsSuccess() = runBlocking {
        val mockResponseJson = """
            {
              "results": [
                {
                  "name": "Berlin",
                  "latitude": 52.52437,
                  "longitude": 13.41053,
                  "country": "Germany",
                  "admin1": "Berlin"
                }
              ]
            }
        """.trimIndent()

        val mockClient = createMockHttpClient(mockResponseJson)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ProviderManager(context, mockClient)

        val results = manager.searchLocations("Berlin")
        assertEquals(1, results.size)
        val first = results[0]
        assertEquals("Berlin", first.name)
        assertEquals(52.52437, first.latitude, 0.0001)
        assertEquals(13.41053, first.longitude, 0.0001)
        assertEquals("Germany", first.country)
        assertEquals("Berlin", first.admin1)
    }

    @Test
    fun testFetchWeatherCachesSuccessfully() = runBlocking {
        val mockWeatherJson = """
            {
              "latitude": 52.52,
              "longitude": 13.41,
              "hourly": {
                "temperature_2m": [18.5, 19.0],
                "weather_code": [0, 1]
              }
            }
        """.trimIndent()

        val mockClient = createMockHttpClient(mockWeatherJson)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ProviderManager(context, mockClient)

        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val devicePrefs =
            context.getSharedPreferences(SettingsRepository.DEVICE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        devicePrefs.edit().clear().commit()

        // A city the user picked, which lives with their settings.
        prefs.edit()
            .putString("weather_location_mode", SettingsRepository.MODE_MANUAL)
            .putString("weather_manual_lat", "52.52")
            .putString("weather_manual_lon", "13.41")
            .commit()

        manager.fetchWeather()

        val cache = devicePrefs.getString(SettingsRepository.KEY_WEATHER_CACHE, null)
        assertNotNull(cache)
        val cachedJson = JSONObject(cache!!)
        assertEquals(52.52, cachedJson.getDouble("latitude"), 0.01)

        val lastUpdate = devicePrefs.getLong(SettingsRepository.KEY_WEATHER_LAST_UPDATE, 0L)
        assertTrue(lastUpdate > 0L)
    }

    @Test
    fun testFetchWeatherIpFallbackWithCity() = runBlocking {
        val mockIpJson = """
            {
              "latitude": 42.3601,
              "longitude": -71.0589,
              "city": "Boston"
            }
        """.trimIndent()

        val mockWeatherJson = """
            {
              "latitude": 42.3601,
              "longitude": -71.0589,
              "hourly": {
                "temperature_2m": [15.0, 16.0],
                "weather_code": [1, 2]
              }
            }
        """.trimIndent()

        val mockClient = createMockHttpClient(mockIpJson, mockWeatherJson)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ProviderManager(context, mockClient)

        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val devicePrefs =
            context.getSharedPreferences(SettingsRepository.DEVICE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        devicePrefs.edit().clear().commit()

        manager.fetchWeather()

        // An IP lookup is something this device worked out, so it stays device-side and
        // never reaches the file that gets backed up.
        assertEquals("Boston", devicePrefs.getString(SettingsRepository.KEY_RESOLVED_CITY, null))
        assertEquals("42.3601", devicePrefs.getString(SettingsRepository.KEY_RESOLVED_LAT, null))
        assertEquals("-71.0589", devicePrefs.getString(SettingsRepository.KEY_RESOLVED_LON, null))
        assertNull(prefs.getString("weather_manual_city", null))

        val cache = devicePrefs.getString(SettingsRepository.KEY_WEATHER_CACHE, null)
        assertNotNull(cache)
        val cachedJson = JSONObject(cache!!)
        assertEquals("Boston", cachedJson.getString("city"))
    }

    private fun createMockHttpClient(responseBody: String): OkHttpClient {
        return createMockHttpClient(responseBody, responseBody)
    }

    private fun createMockHttpClient(ipResponseBody: String, weatherResponseBody: String): OkHttpClient {
        val interceptor = Interceptor { chain ->
            val url = chain.request().url.toString()
            val body = if (url.contains("ipapi.co")) {
                ipResponseBody
            } else {
                weatherResponseBody
            }
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }
}
