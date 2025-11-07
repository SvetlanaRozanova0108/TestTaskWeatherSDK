package ru.practicum.test_task_weather_sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.test_task_weather_sdk.exception.*;
import ru.practicum.test_task_weather_sdk.service.WeatherCacheManager;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class WeatherCacheManagerTest {
    private WeatherCacheManager cacheManager;
    private FakeTicker testTicker;

    @BeforeEach
    void setUp() {
        testTicker = new FakeTicker();
        cacheManager = new WeatherCacheManager(testTicker);
    }

    @Test
    void testAddAndGetData() {
        cacheManager.updateCache("London", "Sunny 15 degrees");
        assertEquals("Sunny 15 degrees", cacheManager.getCached("London"));
    }

    @Test
    void testEmptyWeatherDataThrowsException() {
        assertThrows(WeatherSdkException.class, () -> cacheManager.updateCache("New York", ""));
    }

    @Test
    void testAddMultipleCitiesAndEviction() {
        for (int i = 1; i <= 12; i++) {
            cacheManager.updateCache("City" + i, "Weather" + i);
        }

        assertThrows(CityNotFoundException.class, () -> cacheManager.getCached("City1"));
        assertThrows(CityNotFoundException.class, () -> cacheManager.getCached("City2"));
        assertNotNull(cacheManager.getCached("City3"));
        assertNotNull(cacheManager.getCached("City12"));
    }

    @Test
    void testCacheExpirationWithFakeTicker() {
        cacheManager.updateCache("Madrid", "Sunny 15 degrees");
        testTicker.advance(Duration.ofMinutes(11));
        assertThrows(CityNotFoundException.class, () -> cacheManager.getCached("Madrid"));
    }

    @Test
    void testConcurrencyWith50Threads() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int cityIndex = i;
            executor.execute(() -> {
                try {
                    cacheManager.updateCache("City" + cityIndex, "Weather" + cityIndex);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertTrue(cacheManager.getCachedCities().size() <= 10);
    }

    @Test
    void testUpdateExistingCity() {
        cacheManager.updateCache("Tokyo", "Cloudy 20 degrees");
        cacheManager.updateCache("Tokyo", "Sunny 25 degrees");

        assertEquals("Sunny 25 degrees", cacheManager.getCached("Tokyo"));
    }

    @Test
    void testCityNotFoundThrowsException() {
        assertThrows(CityNotFoundException.class, () -> cacheManager.getCached("NonExistingCity"));
    }

    @Test
    void testInvalidCityNameThrowsException() {
        assertThrows(InvalidCityException.class, () -> cacheManager.updateCache(null, "Weather"));
        assertThrows(InvalidCityException.class, () -> cacheManager.updateCache("  ", "Weather"));
        assertThrows(InvalidCityException.class, () -> cacheManager.getCached("  "));
    }
}
