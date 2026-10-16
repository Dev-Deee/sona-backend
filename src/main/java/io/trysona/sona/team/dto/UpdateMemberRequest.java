package io.trysona.sona.team.dto;

import io.trysona.sona.team.TeamRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRequest(@NotNull TeamRole role) {
}
