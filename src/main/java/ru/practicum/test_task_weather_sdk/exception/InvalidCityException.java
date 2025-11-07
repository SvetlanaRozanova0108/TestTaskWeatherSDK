package ru.practicum.test_task_weather_sdk.exception;

public class InvalidCityException extends WeatherSdkException {
    public InvalidCityException(String message) {
        super(message);
    }
}
