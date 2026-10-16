package io.trysona.sona.user.dto;

import io.trysona.sona.user.UserRole;
import java.util.Map;

/**
 * All fields optional. {@code preferences} is shallow-merged into the stored preferences; a key
 * with a {@code null} value is removed.
 */
public record UpdateUserRequest(UserRole role, Map<String, Object> preferences) {
}
