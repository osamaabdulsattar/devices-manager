package com.example.devicesmanager.exception;

/**
 * Thrown when a request is well-formed but conflicts with the current state of a resource
 * (e.g. a business rule forbids the change). Mapped to {@code 409 Conflict}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
