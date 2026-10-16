package io.trysona.sona.exception;

/**
 * Thrown when a JWT or refresh token has expired (401).
 */
public class ExpiredTokenException extends RuntimeException {

    public ExpiredTokenException(String message) {
        super(message);
    }
}
