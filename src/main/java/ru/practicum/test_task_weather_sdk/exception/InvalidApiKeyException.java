package ru.practicum.test_task_weather_sdk.exception;

public class InvalidApiKeyException extends WeatherSdkException {
    public InvalidApiKeyException(String message) {
        super(message);
    }
}
