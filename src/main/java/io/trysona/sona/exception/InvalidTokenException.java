package io.trysona.sona.exception;

/**
 * Thrown when a JWT or refresh token is malformed, tampered with, revoked or unknown (401).
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
