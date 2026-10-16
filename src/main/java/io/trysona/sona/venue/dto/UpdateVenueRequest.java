package io.trysona.sona.venue.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Every field is optional; omitted (null) fields are left unchanged.
 */
public record UpdateVenueRequest(
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
        UUID teamUuid,
        Map<String, Object> dimensions,
        Map<String, Object> surfaces,
        @Size(max = 10_000) String notes) {
}
