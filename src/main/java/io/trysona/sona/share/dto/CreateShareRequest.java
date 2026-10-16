package io.trysona.sona.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * {@code singleUse}: when true the link works exactly once; defaults to multi-use.
 */
public record CreateShareRequest(
        @NotBlank @Size(max = 100) String calculatorTab,
        @NotNull Map<String, Object> inputs,
        Map<String, Object> outputs,
        Boolean singleUse) {
}
