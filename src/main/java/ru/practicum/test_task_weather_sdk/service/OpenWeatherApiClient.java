package ru.practicum.test_task_weather_sdk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.practicum.test_task_weather_sdk.dto.WeatherResponse;
import ru.practicum.test_task_weather_sdk.exception.*;

import java.util.Map;

@Slf4j
public class OpenWeatherApiClient {

    private static final String BASE_URL = "https://api.openweathermap.org/data/3.0";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;

    public OpenWeatherApiClient(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidApiKeyException("API key cannot be null or empty");
        }
        this.apiKey = apiKey;
        this.webClient = WebClient
                .builder()
                .baseUrl(BASE_URL)
                .build();
    }

    public OpenWeatherApiClient(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidApiKeyException("API key cannot be null or empty");
        }
        this.apiKey = apiKey;
        this.webClient = WebClient
                .builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String fetchWeather(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new InvalidCityException("City name cannot be null or empty");
        }
        try {
            String result = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather")
                            .queryParam("q", cityName)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "imperial")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(errorBody ->
                                            handleErrorResponse(response.statusCode(), errorBody))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(errorBody ->
                                            Mono.error(new UnexpectedApiException(
                                                    "OpenWeather API server error: " + errorBody, null)))
                    )
                    .bodyToMono(WeatherResponse.class)
                    .map(this::convertToRequiredFormat)
                    .onErrorMap(ex -> !(ex instanceof WeatherSdkException),
                            ex -> new NetworkException("Network error: " + ex.getMessage(), ex))
                    .block();

            log.info("Successfully fetched weather for city='{}'", cityName);
            return result;
        } catch (WeatherSdkException e) {
            log.info("WeatherSdkException occurred for city='{}': {}", cityName, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected network error for city='{}': {}", cityName, e.getMessage(), e);
            throw new NetworkException("Unexpected network error: " + e.getMessage(), e);
        }
    }

    private String convertToRequiredFormat(WeatherResponse response) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "weather", Map.of(
                            "main", response.getWeather()[0].getMain(),
                            "description", response.getWeather()[0].getDescription()
                    ),
                    "temperature", Map.of(
                            "temp", response.getTemperature().getTemp(),
                            "feels_like", response.getTemperature().getFeelsLike()
                    ),
                    "visibility", response.getVisibility(),
                    "wind", Map.of(
                            "speed", response.getWind().getSpeed()
                    ),
                    "datetime", response.getDatetime(),
                    "sun", Map.of(
                            "sunrise", response.getSun().getSunrise(),
                            "sunset", response.getSun().getSunset()
                    ),
                    "timezone", response.getTimezone(),
                    "name", response.getName()
            ));
        } catch (Exception e) {
            log.error("Error converting the response to JSON: {}", e.getMessage(), e);
            throw new WeatherSdkException("Error processing weather data", e);
        }
    }

    private Mono<Throwable> handleErrorResponse(HttpStatusCode status, String error) {
        log.warn("Client error from OpenWeather API (status={}): {}", status.value(), error);
        String errorMessage = extractErrorMessage(error);
        return switch (status.value()) {
            case 401 -> Mono.error(new InvalidApiKeyException("Invalid API Key: " + errorMessage));
            case 403 -> Mono.error(new ApiKeyBlockedException("API Key blocked: " + errorMessage));
            case 404 -> Mono.error(new CityNotFoundException("City not found: " + errorMessage));
            default -> Mono.error(new WeatherSdkException("API error: " + errorMessage));
        };
    }

    private String extractErrorMessage(String error) {
        try {
            Map<String, Object> errorMap = OBJECT_MAPPER.readValue(error, Map.class);
            return (String) errorMap.getOrDefault("message", "Unknown error");
        } catch (Exception e) {
            log.error("Failed to parse error response: {}", e.getMessage(), e);
            return "Unknown error (failed to parse response)";
        }
    }
}
