package io.trysona.sona.show.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Every field is optional; omitted (null) fields are left unchanged. {@code calculatorStates} is
 * shallow-merged by calculator tab so saving one tab does not wipe the others.
 */
public record UpdateShowRequest(
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
        LocalDate showDate,
        Map<String, Object> calculatorStates,
        List<Map<String, Object>> inputList,
        @Size(max = 10_000) String notes) {
}
