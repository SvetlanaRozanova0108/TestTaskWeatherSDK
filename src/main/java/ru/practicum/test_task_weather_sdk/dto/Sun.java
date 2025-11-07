package ru.practicum.test_task_weather_sdk.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sun {

    @Min(value = 1, message = "Sunrise time cannot be zero or negative")
    @JsonProperty("sunrise")
    private Long sunrise;

    @Min(value = 1, message = "Sunset time cannot be zero or negative")
    @JsonProperty("sunset")
    private Long sunset;

    Sun() {}
}
