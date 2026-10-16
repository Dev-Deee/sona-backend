package io.trysona.sona.exception;

/**
 * Thrown when an email/password pair does not match (401).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
