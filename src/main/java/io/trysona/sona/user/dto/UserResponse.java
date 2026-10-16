package io.trysona.sona.user.dto;

import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRole;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserResponse(UUID uuid, String email, UserRole role, Map<String, Object> preferences,
                           Instant createdAt, Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getUuid(), user.getEmail(), user.getRole(), user.getPreferences(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
