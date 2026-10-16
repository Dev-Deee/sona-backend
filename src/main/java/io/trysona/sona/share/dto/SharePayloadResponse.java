package io.trysona.sona.share.dto;

import java.time.Instant;
import java.util.Map;

public record SharePayloadResponse(String calculatorTab, Map<String, Object> inputs, Map<String, Object> outputs,
                                   Instant expiresAt) {
}
