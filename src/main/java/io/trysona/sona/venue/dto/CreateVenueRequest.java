package io.trysona.sona.venue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record CreateVenueRequest(
        @NotBlank @Size(max = 255) String name,
        UUID teamUuid,
        Map<String, Object> dimensions,
        Map<String, Object> surfaces,
        @Size(max = 10_000) String notes) {
}
