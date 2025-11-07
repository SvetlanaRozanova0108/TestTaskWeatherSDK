package ru.practicum.test_task_weather_sdk.service;

import lombok.extern.slf4j.Slf4j;
import ru.practicum.test_task_weather_sdk.dto.ModeSDK;
import ru.practicum.test_task_weather_sdk.exception.InvalidApiKeyException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class WeatherSdkFactory {

    private static final ConcurrentHashMap<String, WeatherSdk> instances = new ConcurrentHashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    private WeatherSdkFactory() {
    }

    public static WeatherSdk getInstance(String apiKey, ModeSDK mode, Integer pollingIntervalSeconds) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidApiKeyException("API key cannot be null or empty.");
        }
        lock.lock();
        try {
            if (instances.containsKey(apiKey)) {
                WeatherSdk existingInstance = instances.get(apiKey);
                if (needsUpdate(existingInstance, mode, pollingIntervalSeconds)) {
                    log.info("Parameters have changed, creating a new instance for API key: {}", apiKey);
                    removeInstance(apiKey);
                } else {
                    log.info("Returning existing WeatherSdk instance for API key: {}", apiKey);
                    return existingInstance;
                }
            }
            log.info("Creating new WeatherSdk instance for API key: {}", apiKey);
            return createNewInstance(apiKey, mode, pollingIntervalSeconds);
        } finally {
            lock.unlock();
        }
    }

    private static Boolean needsUpdate(WeatherSdk existingInstance, ModeSDK mode, int pollingIntervalSeconds) {
        WeatherSdkImpl sdkImpl = (WeatherSdkImpl) existingInstance;
        return sdkImpl.getMode() != mode || sdkImpl.getPollingIntervalSeconds() != pollingIntervalSeconds;
    }

    private static WeatherSdk createNewInstance(String apiKey, ModeSDK mode, int pollingIntervalSeconds) {
        WeatherSdk newInstance = new WeatherSdkImpl(apiKey, mode, pollingIntervalSeconds);
        instances.put(apiKey, newInstance);
        return newInstance;
    }

    public static void removeInstance(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidApiKeyException("API key cannot be null or empty.");
        }
        lock.lock();
        try {
            WeatherSdk instance = instances.remove(apiKey);
            if (instance != null) {
                instance.shutdown();
                log.info("Removed WeatherSdk instance for API key: {}", apiKey);
            } else {
                log.warn("Attempted to remove non-existent WeatherSdk instance for API key: {}", apiKey);
                throw new InvalidApiKeyException("No instance found for the given API key.");
            }
        } finally {
            lock.unlock();
        }
    }
}
