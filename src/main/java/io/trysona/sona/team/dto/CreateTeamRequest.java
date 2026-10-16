package io.trysona.sona.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(@NotBlank @Size(max = 255) String name) {
}
