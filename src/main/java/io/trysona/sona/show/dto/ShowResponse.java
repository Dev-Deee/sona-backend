package io.trysona.sona.show.dto;

import io.trysona.sona.show.Show;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ShowResponse(UUID uuid, String name, LocalDate showDate, UUID venueUuid, UUID ownerUuid,
                           Map<String, Object> calculatorStates, List<Map<String, Object>> inputList, String notes,
                           Instant createdAt, Instant updatedAt) {

    public static ShowResponse from(Show show) {
        return new ShowResponse(
                show.getUuid(),
                show.getName(),
                show.getShowDate(),
                show.getVenue() == null ? null : show.getVenue().getUuid(),
                show.getOwner() == null ? null : show.getOwner().getUuid(),
                show.getCalculatorStates(),
                show.getInputList(),
                show.getNotes(),
                show.getCreatedAt(),
                show.getUpdatedAt());
    }
}
