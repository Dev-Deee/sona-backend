package io.trysona.sona.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope.
 */
public record ApiError(boolean success, String error, String message, List<FieldIssue> details, Instant timestamp) {

    public record FieldIssue(String field, String message) {
    }

    public static ApiError of(String error, String message) {
        return new ApiError(false, error, message, List.of(), Instant.now());
    }

    public static ApiError of(String error, String message, List<FieldIssue> details) {
        return new ApiError(false, error, message, details, Instant.now());
    }
}
