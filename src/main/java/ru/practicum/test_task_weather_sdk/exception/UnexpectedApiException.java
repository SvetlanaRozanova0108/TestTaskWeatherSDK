package ru.practicum.test_task_weather_sdk.exception;

public class UnexpectedApiException extends WeatherSdkException {
    public UnexpectedApiException(String message, Throwable cause) {
        super(message, 0, null, cause);
    }
}
