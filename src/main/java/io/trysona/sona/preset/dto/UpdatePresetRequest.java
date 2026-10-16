package io.trysona.sona.preset.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Every field is optional; omitted (null) fields are left unchanged.
 */
public record UpdatePresetRequest(
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
        @Pattern(regexp = PresetConstraints.TAB_PATTERN, message = PresetConstraints.TAB_MESSAGE)
        String calculatorTab,
        Map<String, Object> inputs,
        Boolean isPublic) {
}
