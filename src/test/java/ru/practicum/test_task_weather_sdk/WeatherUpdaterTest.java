package ru.practicum.test_task_weather_sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.test_task_weather_sdk.exception.WeatherSdkException;
import ru.practicum.test_task_weather_sdk.service.OpenWeatherApiClient;
import ru.practicum.test_task_weather_sdk.service.WeatherCacheManager;
import ru.practicum.test_task_weather_sdk.service.WeatherUpdater;

import java.util.Set;
import java.util.concurrent.*;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherUpdaterTest {
    private WeatherCacheManager cacheManager;
    private OpenWeatherApiClient apiClient;
    private WeatherUpdater weatherUpdater;

    @BeforeEach
    void setUp() {
        cacheManager = mock(WeatherCacheManager.class);
        apiClient = mock(OpenWeatherApiClient.class);
    }

    @Test
    void testUpdaterStartsAndStopsCorrectly() {
        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 2);
        assertFalse(isSchedulerShutdown(weatherUpdater));

        weatherUpdater.stop();
        assertTrue(isSchedulerShutdown(weatherUpdater));
    }

    @Test
    void testWeatherUpdaterUpdatesCache() {
        when(cacheManager.getCachedCities()).thenReturn(Set.of("London", "Paris"));
        when(apiClient.fetchWeather("London")).thenReturn("{\"weather\": {\"main\": \"Clouds\"}}");
        when(apiClient.fetchWeather("Paris")).thenReturn("{\"weather\": {\"main\": \"Clear\"}}");

        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(apiClient, atLeastOnce()).fetchWeather("London");
            verify(apiClient, atLeastOnce()).fetchWeather("Paris");
        });

        weatherUpdater.stop();
    }

    @Test
    void testUpdaterHandlesExceptionsGracefully() {
        when(cacheManager.getCachedCities()).thenReturn(Set.of("New York"));
        when(apiClient.fetchWeather("New York")).thenThrow(new WeatherSdkException("API Error"));

        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> verify(apiClient, atLeastOnce()).fetchWeather("New York"));

        weatherUpdater.stop();
    }

    @Test
    void testUpdaterSkipsEmptyCache() {
        when(cacheManager.getCachedCities()).thenReturn(Set.of());
        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> verify(apiClient, never()).fetchWeather(anyString()));

        weatherUpdater.stop();
    }

    @Test
    void testUpdaterThreadSafety() throws InterruptedException {
        when(cacheManager.getCachedCities()).thenReturn(Set.of("Berlin"));
        when(apiClient.fetchWeather("Berlin")).thenReturn("{\"weather\": {\"main\": \"Sunny\"}}");

        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++)
            executor.submit(() -> {
                weatherUpdater.updateWeather();
                latch.countDown();
            });

        latch.await();
        executor.shutdown();
        verify(apiClient, atMost(5)).fetchWeather("Berlin");

        weatherUpdater.stop();
    }

    @Test
    void testUpdaterDoesNotRunAfterStop() {
        when(cacheManager.getCachedCities()).thenReturn(Set.of("Madrid"));
        when(apiClient.fetchWeather("Madrid")).thenReturn("{\"weather\": {\"main\": \"Sunny\"}}");

        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> verify(apiClient, atLeastOnce()).fetchWeather("Madrid"));

        weatherUpdater.stop();
        await().during(2, TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                verify(apiClient, times(1)).fetchWeather("Madrid")
        );
    }

    @Test
    void testUpdaterThrowsExceptionForInvalidInterval() {
        assertThrows(WeatherSdkException.class, () -> new WeatherUpdater(cacheManager, apiClient, 0));
        assertThrows(WeatherSdkException.class, () -> new WeatherUpdater(cacheManager, apiClient, -5));
    }

    @Test
    void testStop_ShutsDownScheduler()  {
        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);

        weatherUpdater.stop();

        ScheduledExecutorService scheduler = extractScheduler(weatherUpdater);
        assertTrue(scheduler.isShutdown());
    }

    @Test
    void testStop_ForceShutdownIfSchedulerNotTerminating()  {
        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);

        ScheduledExecutorService scheduler = extractScheduler(weatherUpdater);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {}, 10, TimeUnit.SECONDS);

        weatherUpdater.stop();

        assertTrue(scheduler.isShutdown());
    }

    @Test
    void testStop_HandlesInterruptedException() {
        weatherUpdater = new WeatherUpdater(cacheManager, apiClient, 1);
        ScheduledExecutorService scheduler = extractScheduler(weatherUpdater);

        Thread.currentThread().interrupt();

        weatherUpdater.stop();

        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(scheduler.isShutdown());
    }

    private boolean isSchedulerShutdown(WeatherUpdater updater) {
        ScheduledExecutorService scheduler = extractScheduler(updater);
        return scheduler.isShutdown();
    }

    private ScheduledExecutorService extractScheduler(WeatherUpdater updater) {
        try {
            var field = WeatherUpdater.class.getDeclaredField("scheduler");
            field.setAccessible(true);
            return (ScheduledExecutorService) field.get(updater);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access scheduler field", e);
        }
    }
}
