package ru.practicum.test_task_weather_sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.test_task_weather_sdk.dto.ModeSDK;
import ru.practicum.test_task_weather_sdk.exception.*;
import ru.practicum.test_task_weather_sdk.service.OpenWeatherApiClient;
import ru.practicum.test_task_weather_sdk.service.WeatherCacheManager;
import ru.practicum.test_task_weather_sdk.service.WeatherSdkImpl;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WeatherSdkImplTest {
    private WeatherSdkImpl sdk;
    private OpenWeatherApiClient apiClientMock;
    private WeatherCacheManager cacheManagerMock;

    @BeforeEach
    void setUp() {
        apiClientMock = mock(OpenWeatherApiClient.class);
        cacheManagerMock = mock(WeatherCacheManager.class);

        sdk = new WeatherSdkImpl("validKey", ModeSDK.ON_DEMAND_MODE, 0);

        ReflectionTestUtils.setField(sdk, "apiClient", apiClientMock);
        ReflectionTestUtils.setField(sdk, "cacheManager", cacheManagerMock);
    }

    @Test
    void testConstructorThrowsExceptionForInvalidApiKey() {
        assertThrows(WeatherSdkException.class, () -> new WeatherSdkImpl("", ModeSDK.ON_DEMAND_MODE, 0));
        assertThrows(WeatherSdkException.class, () -> new WeatherSdkImpl("   ", ModeSDK.POLLING_MODE, 10));
    }

    @Test
    void testShouldThrowExceptionWhenPollingIntervalSetForOnDemandMode() {
        assertThrows(WeatherSdkException.class, () -> new WeatherSdkImpl("validKey", ModeSDK.ON_DEMAND_MODE, 10));
    }

    @Test
    void testShouldThrowExceptionWhenNonPollingModeHasPositivePollingInterval() {
        assertThrows(WeatherSdkException.class, () -> new WeatherSdkImpl("validKey", ModeSDK.ON_DEMAND_MODE, 5));
    }

    @Test
    void testShouldInitializeCorrectlyForPollingMode() {
        WeatherSdkImpl weatherSdk = new WeatherSdkImpl("validKey", ModeSDK.POLLING_MODE, 10);
        assertNotNull(weatherSdk);
    }

    @Test
    void testShouldInitializeCorrectlyForOnDemandMode() {
        WeatherSdkImpl weatherSdk = new WeatherSdkImpl("validKey", ModeSDK.ON_DEMAND_MODE, 0);
        assertNotNull(weatherSdk);
    }

    @Test
    void testGetWeatherReturnsCachedData() {
        when(cacheManagerMock.getCached("London")).thenReturn("cached-london");
        String result = sdk.getWeather("London");
        assertEquals("cached-london", result);
        verify(cacheManagerMock).getCached("London");
        verifyNoMoreInteractions(cacheManagerMock);
    }

    @Test
    void testGetWeatherFetchesFromApiIfNotCached() {
        when(cacheManagerMock.getCached("Zocca")).thenReturn(null);
        when(apiClientMock.fetchWeather("Zocca")).thenReturn("""
                {"weather": {"main": "Clouds","description": "overcast clouds"},
                  "temperature": {"temp": 48.78,"feels_like": 48.78},
                  "visibility": 10000,
                  "wind": {"speed": 2.46},
                  "datetime": 1740406884,
                  "sys": {"sunrise": 1740376916, "sunset": 1740416215},
                  "timezone": 3600,
                  "name": "Zocca"}
                """);

        String result = sdk.getWeather("Zocca");
        assertNotNull(result);
        verify(cacheManagerMock, times(2)).getCached("Zocca");
        verify(apiClientMock).fetchWeather("Zocca");
        verify(cacheManagerMock).updateCache(eq("Zocca"), anyString());
    }


    @Test
    void testGetCachedCitiesReturnsCityList() {
        when(cacheManagerMock.getCachedCities()).thenReturn(Set.of("London", "New York", "Tokyo"));

        List<String> cities = sdk.getCachedCities();

        assertNotNull(cities);
        assertEquals(3, cities.size());
        assertTrue(cities.contains("London"));
        assertTrue(cities.contains("New York"));
        assertTrue(cities.contains("Tokyo"));
    }

    @Test
    void testGetCachedCitiesReturnsEmptyListWhenNoCitiesCach() {
        when(cacheManagerMock.getCachedCities()).thenReturn(Set.of());

        List<String> cities = sdk.getCachedCities();

        assertNotNull(cities);
        assertTrue(cities.isEmpty());
    }

    @Test
    void testGetWeatherThrowsInvalidCityException() {
        assertThrows(InvalidCityException.class, () -> sdk.getWeather(null));
        assertThrows(InvalidCityException.class, () -> sdk.getWeather("  "));
    }

    @Test
    void testStopPollingWithInterval() {
        WeatherSdkImpl pollingSdk = new WeatherSdkImpl("validKey", ModeSDK.POLLING_MODE, 5);
        assertTrue(pollingSdk.isPollingEnabled());
        pollingSdk.stopPolling();
        assertFalse(pollingSdk.isPollingEnabled());
    }

    @Test
    void testStopPollingWithZeroInterval() {
        assertFalse(sdk.isPollingEnabled());
        sdk.stopPolling();
        assertFalse(sdk.isPollingEnabled());
    }

    @Test
    void testClearCache() {
        assertDoesNotThrow(() -> sdk.clearCache());
        verify(cacheManagerMock).clearCache();
    }

    @Test
    void testUpdateWeather() {
        when(apiClientMock.fetchWeather("Zocca"))
                .thenReturn("""
                        {"weather":{"main":"Clouds","description":"overcast clouds"},
                        "temperature":{"temp":48.78,"feels_like":48.78},
                        "visibility":10000,
                        "wind":{"speed":2.46},
                        "datetime":1740406884,
                        "sys":{"sunrise":1740376916,"sunset":1740416215},
                        "timezone":3600,
                        "name":"Zocca"}
                        """);
        when(cacheManagerMock.getCached("Moscow")).thenReturn(null);

        assertDoesNotThrow(() -> sdk.updateWeather("Zocca"));
        verify(cacheManagerMock).getCached("Zocca");
        verify(apiClientMock).fetchWeather("Zocca");
        verify(cacheManagerMock).updateCache(eq("Zocca"), anyString());
    }

    @Test
    void testGetWeatherWhenApiClientThrows() {
        when(cacheManagerMock.getCached("ExceptionCity")).thenReturn(null);
        when(apiClientMock.fetchWeather("ExceptionCity")).thenThrow(new WeatherSdkException("Network error"));

        assertThrows(WeatherSdkException.class, () -> sdk.getWeather("ExceptionCity"));

        verify(cacheManagerMock, times(2)).getCached("ExceptionCity");
        verify(apiClientMock).fetchWeather("ExceptionCity");
        verify(cacheManagerMock, never()).updateCache(anyString(), anyString());
    }

    @Test
    void testGetWeatherFetchesFromApiIfDataIsStale() {
        when(cacheManagerMock.getCached("StaleCity"))
                .thenThrow(new CityNotFoundException("Data is stale or city not found"));
        when(apiClientMock.fetchWeather("StaleCity")).thenReturn("{ \"weather\": [{\"main\": \"Clouds\", \"description\": \"...\"}], \"name\": \"StaleCity\" }");

        String result = sdk.getWeather("StaleCity");
        assertNotNull(result);

        verify(cacheManagerMock, times(2)).getCached("StaleCity");
        verify(apiClientMock).fetchWeather("StaleCity");
        verify(cacheManagerMock).updateCache(eq("StaleCity"), anyString());
    }

    @Test
    void testCacheLimitExceeded() {
        when(cacheManagerMock.getCached("City11")).thenReturn(null);
        doThrow(new WeatherSdkException("Cache limit exceeded"))
                .when(cacheManagerMock).updateCache(eq("City11"), anyString());

        when(apiClientMock.fetchWeather("City11"))
                .thenReturn("{ \"weather\":[{\"main\":\"Clouds\", \"description\":\"...\"}], \"name\":\"City11\" }");

        assertThrows(WeatherSdkException.class, () -> sdk.getWeather("City11"));

        verify(cacheManagerMock, times(2)).getCached("City11");
        verify(apiClientMock).fetchWeather("City11");
        verify(cacheManagerMock).updateCache(eq("City11"), anyString());
    }
}
