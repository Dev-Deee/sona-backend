package io.trysona.sona.auth.dto;

import io.trysona.sona.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes, so longer passwords are rejected rather than truncated.
        @NotBlank @Size(min = 8, max = 72) String password,
        UserRole role) {

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", role=" + role + "]";
    }
}
