package ru.practicum.test_task_weather_sdk.exception;

public class JsonParsingException extends WeatherSdkException {
    public JsonParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
