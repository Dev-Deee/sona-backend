package io.trysona.sona.team.dto;

import io.trysona.sona.team.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotBlank @Email String email, @NotNull TeamRole role) {
}
