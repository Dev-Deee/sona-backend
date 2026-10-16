package io.trysona.sona.auth;

import java.util.UUID;

/**
 * The authenticated principal placed in the security context by the JWT filter.
 */
public record AuthUser(Long id, UUID uuid, String email) {
}
