package io.trysona.sona.share.dto;

import java.time.Instant;

public record ShareCreatedResponse(String token, Instant expiresAt, boolean singleUse) {
}
