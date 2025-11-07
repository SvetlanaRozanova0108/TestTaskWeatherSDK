package ru.practicum.test_task_weather_sdk.exception;

public class CityNotFoundException extends WeatherSdkException {
    public CityNotFoundException(String message) {
        super(message);
    }
}
