package io.trysona.sona.venue.dto;

import io.trysona.sona.venue.Venue;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record VenueResponse(UUID uuid, String name, UUID ownerUuid, UUID teamUuid, Map<String, Object> dimensions,
                            Map<String, Object> surfaces, String notes, Instant createdAt, Instant updatedAt) {

    public static VenueResponse from(Venue venue) {
        return new VenueResponse(
                venue.getUuid(),
                venue.getName(),
                venue.getOwner() == null ? null : venue.getOwner().getUuid(),
                venue.getTeam() == null ? null : venue.getTeam().getUuid(),
                venue.getDimensions(),
                venue.getSurfaces(),
                venue.getNotes(),
                venue.getCreatedAt(),
                venue.getUpdatedAt());
    }
}
