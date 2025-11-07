package ru.practicum.test_task_weather_sdk.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.test_task_weather_sdk.exception.*;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class WeatherCacheManager {

    private static final int MAX_CITIES = 10;
    private static final Duration EXPIRATION_TIME = Duration.ofMinutes(10);

    private final Cache<String, String> cache;
    private final ConcurrentLinkedQueue<String> cityOrder = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Boolean> citySet = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    WeatherCacheManager() {
        this(Ticker.systemTicker());
    }

    public WeatherCacheManager(Ticker ticker) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_CITIES)
                .expireAfterWrite(EXPIRATION_TIME)
                .ticker(ticker)
                .build();
    }

    public String getCached(final String cityName) {
        validateCityName(cityName);

        String data = cache.getIfPresent(cityName);
        if (data == null) {
            log.warn("Weather for city '{}' is not found in cache", cityName);
            throw new CityNotFoundException("Weather for city '" + cityName + "' is not found in cache");
        }
        log.info("Weather for city '{}' retrieved from cache", cityName);
        return data;
    }

    public void updateCache(final String cityName, final String data) {
        validateCityName(cityName);
        if (data == null || data.trim().isEmpty()) {
            throw new WeatherSdkException("Weather cannot be null or empty for city: " + cityName);
        }
        Boolean alreadyExists = citySet.putIfAbsent(cityName, true) != null;
        cache.put(cityName, data);
        synchronized (this) {
            if (!alreadyExists) {
                cityOrder.add(cityName);
                log.info("Added new city '{}' to cache", cityName);
            } else {
                log.debug("Updated weather for city '{}'", cityName);
            }
            if (cityOrder.size() > MAX_CITIES) {
                removeOldestEntry();
            }
        }

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            cache.invalidate(cityName);
            citySet.remove(cityName);
            log.info("Cache expired and removed for '{}'", cityName);
        }, EXPIRATION_TIME.toMinutes(), TimeUnit.MINUTES);
    }

    public Set<String> getCachedCities() {
        return cache.asMap().keySet();
    }

    public void clearCache() {
        lock.lock();
        try {
            cache.invalidateAll();
            cityOrder.clear();
            citySet.clear();
            log.info("Cache cleared");
        } finally {
            lock.unlock();
        }
    }

    private void removeOldestEntry() {
        lock.lock();
        try {
            while (cityOrder.size() > MAX_CITIES) {
                String oldestCity = cityOrder.poll();
                if (oldestCity != null) {
                    cache.invalidate(oldestCity);
                    citySet.remove(oldestCity);
                    log.debug("Evicted oldest city '{}' from cache", oldestCity);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void validateCityName(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new InvalidCityException("City name cannot be null or empty");
        }
    }
}
