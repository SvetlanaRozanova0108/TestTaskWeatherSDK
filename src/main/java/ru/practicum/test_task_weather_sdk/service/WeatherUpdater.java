package ru.practicum.test_task_weather_sdk.service;

import lombok.extern.slf4j.Slf4j;
import ru.practicum.test_task_weather_sdk.exception.WeatherSdkException;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class WeatherUpdater {

    private final WeatherCacheManager cacheManager;
    private final OpenWeatherApiClient apiClient;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock lock = new ReentrantLock();

    public WeatherUpdater(WeatherCacheManager cacheManager, OpenWeatherApiClient apiClient, Integer interval) {
        if (interval <= 0) throw new WeatherSdkException("Polling interval must be greater than 0 seconds");
        this.cacheManager = cacheManager;
        this.apiClient = apiClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::updateWeather, 0, interval, TimeUnit.SECONDS);
        log.info("WeatherUpdater started with polling interval of {} seconds", interval);
    }

    public synchronized void stop() {
        if (scheduler != null) {
            log.info("Stop WeatherUpdater");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Scheduler did not terminate in time, forcing shutdown");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Shutdown interrupted, forcing termination");
                scheduler.shutdownNow();
            }
            log.info("WeatherUpdater stopped");
        }
    }

    public void updateWeather() {
        if (!lock.tryLock()) {
            log.warn("Skipping update, another update is already in progress");
            return;
        }
        try {
            log.debug("Fetching cached cities");
            Set<String> cities = cacheManager.getCachedCities();
            if (cities.isEmpty()) {
                log.info("No cities in cache, skipping update");
                return;
            }

            log.info("ModeSDK weather for {} cities", cities.size());
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(cities.size(), 5));
            CountDownLatch latch = new CountDownLatch(cities.size());

            for (String city : cities) {
                executor.submit(() -> {
                    try {
                        String weatherData = apiClient.fetchWeather(city);
                        cacheManager.updateCache(city, weatherData);
                        log.info("Successfully updated weather for '{}'.", city);
                    } catch (Exception e) {
                        log.error("Failed to update weather for '{}': {}", city, e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
            log.info("Weather update cycle completed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Weather update thread was interrupted.", e);
        } finally {
            lock.unlock();
        }
    }
}
