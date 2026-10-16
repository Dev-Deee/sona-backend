package io.trysona.sona.preset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record CreatePresetRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Pattern(regexp = PresetConstraints.TAB_PATTERN, message = PresetConstraints.TAB_MESSAGE)
        String calculatorTab,
        @NotNull Map<String, Object> inputs,
        UUID teamUuid,
        Boolean isPublic) {
}
