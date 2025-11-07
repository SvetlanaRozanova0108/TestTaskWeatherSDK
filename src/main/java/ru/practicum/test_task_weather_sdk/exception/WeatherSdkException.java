package ru.practicum.test_task_weather_sdk.exception;

import lombok.Getter;

@Getter
public class WeatherSdkException extends RuntimeException {

    private final Integer statusCode;
    private final String errorDetails;

    public WeatherSdkException(String message) {
        super(message);
        this.statusCode = 0;
        this.errorDetails = null;
    }

    public WeatherSdkException(String message, Integer statusCode, String errorDetails) {
        super(message);
        this.statusCode = statusCode;
        this.errorDetails = errorDetails;
    }

    public WeatherSdkException(String message, Integer statusCode, String errorDetails, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorDetails = errorDetails;
    }

    public WeatherSdkException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.errorDetails = null;
    }
}
