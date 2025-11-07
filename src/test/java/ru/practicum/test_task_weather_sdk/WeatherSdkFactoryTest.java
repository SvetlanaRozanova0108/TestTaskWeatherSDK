package ru.practicum.test_task_weather_sdk;

import org.junit.jupiter.api.Test;
import ru.practicum.test_task_weather_sdk.dto.ModeSDK;
import ru.practicum.test_task_weather_sdk.exception.InvalidApiKeyException;
import ru.practicum.test_task_weather_sdk.service.WeatherSdk;
import ru.practicum.test_task_weather_sdk.service.WeatherSdkFactory;

import static org.junit.jupiter.api.Assertions.*;

class WeatherSdkFactoryTest {

    @Test
    void testFactoryCreatesSingleInstancePerApiKey() {
        WeatherSdk sdk1 = WeatherSdkFactory.getInstance("test-key", ModeSDK.POLLING_MODE, 10);
        WeatherSdk sdk2 = WeatherSdkFactory.getInstance("test-key", ModeSDK.POLLING_MODE, 10);

        assertSame(sdk1, sdk2, "Factory should return the same instance for the same API key");

        WeatherSdkFactory.removeInstance("test-key");
    }

    @Test
    void testFactoryOverwritesOldPollingIntervalValueWithNewOneForApiKey() {
        WeatherSdk sdk1 = WeatherSdkFactory.getInstance("test-key", ModeSDK.POLLING_MODE, 10);
        WeatherSdk sdk2 = WeatherSdkFactory.getInstance("test-key", ModeSDK.POLLING_MODE, 15);

        assertNotSame(sdk1, sdk2, "Factory should return a different instance for the same API key");

        WeatherSdkFactory.removeInstance("test-key");
    }

    @Test
    void testFactoryOverwritesOldModeValueWithNewOneForApiKey() {
        WeatherSdk sdk1 = WeatherSdkFactory.getInstance("test-key", ModeSDK.POLLING_MODE, 10);
        WeatherSdk sdk2 = WeatherSdkFactory.getInstance("test-key", ModeSDK.POLLING_MODE, 0);

        assertNotSame(sdk1, sdk2, "Factory should return a different instance for the same API key");

        WeatherSdkFactory.removeInstance("test-key");
    }

    @Test
    void testFactoryCreatesDifferentInstancesForDifferentKeys() {
        WeatherSdk sdk1 = WeatherSdkFactory.getInstance("key1", ModeSDK.ON_DEMAND_MODE, 0);
        WeatherSdk sdk2 = WeatherSdkFactory.getInstance("key2", ModeSDK.ON_DEMAND_MODE, 0);

        assertNotSame(sdk1, sdk2, "Factory should create different instances for different API keys");

        WeatherSdkFactory.removeInstance("key1");
        WeatherSdkFactory.removeInstance("key2");
    }

    @Test
    void testRemoveInstanceDeletesInstance() {
        WeatherSdk sdk = WeatherSdkFactory.getInstance("removable-key", ModeSDK.ON_DEMAND_MODE, 0);
        assertDoesNotThrow(() -> WeatherSdkFactory.removeInstance("removable-key"));

        WeatherSdk newSdk = WeatherSdkFactory.getInstance("removable-key", ModeSDK.ON_DEMAND_MODE, 0);
        assertNotSame(sdk, newSdk, "Factory should create a new instance after removal");

        WeatherSdkFactory.removeInstance("removable-key");
    }

    @Test
    void testRemoveNonExistentInstanceThrowsException() {
        assertThrows(InvalidApiKeyException.class, () -> WeatherSdkFactory.removeInstance("non-existent-key"));
    }

    @Test
    void testFactoryRejectsInvalidApiKey() {
        assertThrows(InvalidApiKeyException.class, () -> WeatherSdkFactory.getInstance("", ModeSDK.ON_DEMAND_MODE, 0));
        assertThrows(InvalidApiKeyException.class, () -> WeatherSdkFactory.getInstance("   ", ModeSDK.ON_DEMAND_MODE, 10));
        assertThrows(InvalidApiKeyException.class, () -> WeatherSdkFactory.getInstance(null, ModeSDK.ON_DEMAND_MODE, 5));
    }
}
