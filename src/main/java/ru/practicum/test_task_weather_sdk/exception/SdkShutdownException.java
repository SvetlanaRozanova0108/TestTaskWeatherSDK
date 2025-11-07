package ru.practicum.test_task_weather_sdk.exception;

public class SdkShutdownException extends WeatherSdkException {
    public SdkShutdownException(String message) {
        super(message);
    }
}
