package io.trysona.sona.exception;

/**
 * Thrown when a resource existed but is no longer available, e.g. an expired share link (410).
 */
public class GoneException extends RuntimeException {

    public GoneException(String message) {
        super(message);
    }
}
