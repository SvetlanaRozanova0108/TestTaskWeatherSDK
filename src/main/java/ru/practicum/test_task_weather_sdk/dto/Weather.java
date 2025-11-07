package ru.practicum.test_task_weather_sdk.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Weather {

    @NotBlank(message = "Main weather condition cannot be empty")
    @JsonProperty("main")
    private String main;

    @NotBlank(message = "Weather description cannot be empty")
    @JsonProperty("description")
    private String description;

    Weather() {}
}
