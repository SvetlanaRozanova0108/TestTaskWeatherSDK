package ru.practicum.test_task_weather_sdk.exception;

public class NetworkException extends WeatherSdkException {
    public NetworkException(String message, Throwable cause) {
        super(message, 0, null, cause);
    }
}
