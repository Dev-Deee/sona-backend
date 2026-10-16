package io.trysona.sona.exception;

/**
 * Thrown when registering with an email that is already taken (409).
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("An account with this email already exists");
    }
}
