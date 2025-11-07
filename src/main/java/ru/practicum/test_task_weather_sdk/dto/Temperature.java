package ru.practicum.test_task_weather_sdk.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Temperature {

    @Min(value = 0, message = "Temperature cannot be negative")
    @JsonProperty("temp")
    private Double temp;

    @Min(value = 0, message = "Feels-like temperature cannot be negative")
    @JsonProperty("feels_like")
    private Double feelsLike;

    Temperature() {}
}
