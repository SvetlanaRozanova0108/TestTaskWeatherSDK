package ru.practicum.test_task_weather_sdk.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Wind {

    @Min(value = 0, message = "Wind speed cannot be negative")
    @JsonProperty("speed")
    private Double speed;

    Wind() {}
}
