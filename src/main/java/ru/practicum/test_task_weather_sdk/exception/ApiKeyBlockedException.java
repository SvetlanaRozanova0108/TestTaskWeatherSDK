package ru.practicum.test_task_weather_sdk.exception;

public class ApiKeyBlockedException extends WeatherSdkException {
    public ApiKeyBlockedException(String message) {
        super(message);
    }
}
