package ru.practicum.test_task_weather_sdk.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    @Valid
    @NotNull(message = "Weather cannot be null")
    @JsonProperty("weather")
    private Weather[] weather;

    @Valid
    @NotNull(message = "Temperature cannot be null")
    @JsonProperty("temperature")
    private Temperature temperature;

    @Min(value = 1, message = "Visibility cannot be zero or negative")
    @JsonProperty("visibility")
    private Integer visibility;

    @Valid
    @NotNull(message = "Wind cannot be null")
    @JsonProperty("wind")
    private Wind wind;

    @Min(value = 1, message = "DateTime cannot be zero or negative")
    @JsonProperty("datetime")
    private Long datetime;

    @Valid
    @NotNull(message = "Sun cannot be null")
    @JsonProperty("sun")
    private Sun sun;

    @Min(value = 1, message = "Timezone cannot be zero or negative")
    @JsonProperty("timezone")
    private Integer timezone;

    @NotBlank(message = "Name cannot be empty")
    @JsonProperty("name")
    private String name;

    WeatherResponse() {}
}
