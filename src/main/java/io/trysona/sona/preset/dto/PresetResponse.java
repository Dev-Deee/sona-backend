package io.trysona.sona.preset.dto;

import io.trysona.sona.preset.Preset;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PresetResponse(UUID uuid, String name, String calculatorTab, Map<String, Object> inputs,
                             UUID ownerUuid, UUID teamUuid, boolean isPublic, Instant createdAt) {

    public static PresetResponse from(Preset preset) {
        return new PresetResponse(
                preset.getUuid(),
                preset.getName(),
                preset.getCalculatorTab(),
                preset.getInputs(),
                preset.getOwner() == null ? null : preset.getOwner().getUuid(),
                preset.getTeam() == null ? null : preset.getTeam().getUuid(),
                preset.isPublicPreset(),
                preset.getCreatedAt());
    }
}
