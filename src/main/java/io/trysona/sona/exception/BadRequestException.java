package io.trysona.sona.exception;

/**
 * Thrown when a request is syntactically valid but semantically wrong (400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
