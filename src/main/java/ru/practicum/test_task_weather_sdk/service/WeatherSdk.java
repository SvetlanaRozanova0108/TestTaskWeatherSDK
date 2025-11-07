package ru.practicum.test_task_weather_sdk.service;

import java.util.List;

public interface WeatherSdk {

    String getWeather(String cityName);

    List<String> getCachedCities();

    boolean isPollingEnabled();

    void updateWeather(String cityName);

    void clearCache();

    void stopPolling();

    void shutdown();
}
