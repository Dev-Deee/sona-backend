package io.trysona.sona.show.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CreateShowRequest(
        @NotBlank @Size(max = 255) String name,
        LocalDate showDate,
        Map<String, Object> calculatorStates,
        List<Map<String, Object>> inputList,
        @Size(max = 10_000) String notes) {
}
