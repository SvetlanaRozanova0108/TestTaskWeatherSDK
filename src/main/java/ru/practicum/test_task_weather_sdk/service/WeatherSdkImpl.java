package ru.practicum.test_task_weather_sdk.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.test_task_weather_sdk.dto.ModeSDK;
import ru.practicum.test_task_weather_sdk.exception.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Getter
public class WeatherSdkImpl implements WeatherSdk {

    private final OpenWeatherApiClient apiClient;
    private final WeatherCacheManager cacheManager;
    private final ReentrantLock lock = new ReentrantLock();
    private WeatherUpdater weatherUpdater;
    private final ModeSDK mode;
    private final Integer pollingIntervalSeconds;
    private volatile Boolean isShutdown = false;

    public WeatherSdkImpl(String apiKey, ModeSDK mode, Integer pollingIntervalSeconds) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new WeatherSdkException("API key cannot be null or empty");
        }
        this.apiClient = new OpenWeatherApiClient(apiKey);
        this.cacheManager = new WeatherCacheManager();

        this.mode = mode;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
        switch (mode) {
            case POLLING_MODE:
                if (pollingIntervalSeconds <= 0) {
                    throw new WeatherSdkException("Polling interval must be greater than 0 seconds");
                }
                this.weatherUpdater = new WeatherUpdater(cacheManager, apiClient, pollingIntervalSeconds);
                log.info("Initialized with POLLING_MODE mode, interval={}s", pollingIntervalSeconds);
                break;
            case ON_DEMAND_MODE:
                if (pollingIntervalSeconds != 0) {
                    throw new WeatherSdkException("Polling interval must be 0 in ON_DEMAND_MODE mode");
                }
                log.info("Initialized in ON_DEMAND_MODE mode (no polling)");
                break;
            default:
                throw new WeatherSdkException("Unsupported update mode");
        }
    }

    @Override
    public String getWeather(String cityName) {
        checkShutdown();
        validateCityName(cityName);
        try {
            String cached = cacheManager.getCached(cityName);
            if (cached != null) {
                log.debug("Returning cached weather for '{}'", cityName);
                return cached;
            }
        } catch (CityNotFoundException e) {
            log.debug("City '{}' not found in cache, fetching from API", cityName);
        }
        return fetchAndCache(cityName);
    }

    @Override
    public void updateWeather(String cityName) {
        checkShutdown();
        validateCityName(cityName);
        fetchAndCache(cityName);
    }

    @Override
    public List<String> getCachedCities() {
        checkShutdown();
        Set<String> set = cacheManager.getCachedCities();
        return new ArrayList<>(set);
    }

    @Override
    public void clearCache() {
        checkShutdown();
        log.info("Clearing cache");
        cacheManager.clearCache();
    }

    @Override
    public boolean isPollingEnabled() {
        checkShutdown();
        return weatherUpdater != null;
    }

    @Override
    public void stopPolling() {
        checkShutdown();
        if (weatherUpdater != null) {
            weatherUpdater.stop();
            weatherUpdater = null;
            log.info("Polling stopped");
        }
    }

    @Override
    public void shutdown() {
        if (!isShutdown) {
            stopPolling();
            clearCache();
            isShutdown = true;
            log.info("WeatherSdk instance has been shut down");
        }
    }

    private String fetchAndCache(String cityName) {
        lock.lock();
        try {
            try {
                String cached = cacheManager.getCached(cityName);
                if (cached != null) {
                    log.debug("Found city '{}' in cache during fetchAndCache, returning it", cityName);
                    return cached;
                }
            } catch (CityNotFoundException e) {
                log.debug("City '{}' not in cache, proceeding to fetch from API", cityName);
            }
            String apiResponse = apiClient.fetchWeather(cityName);
            cacheManager.updateCache(cityName, apiResponse);
            log.info("Fetched and cached weather for '{}'", cityName);
            return apiResponse;
        } finally {
            lock.unlock();
        }
    }

    private void validateCityName(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new InvalidCityException("City name cannot be null or empty");
        }
    }

    private void checkShutdown() {
        if (isShutdown) {
            throw new SdkShutdownException("This WeatherSdk instance has been shut down and cannot be used");
        }
    }
}
