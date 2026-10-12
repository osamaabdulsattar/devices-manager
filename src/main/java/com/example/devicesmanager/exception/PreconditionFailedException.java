package com.example.devicesmanager.exception;

/**
 * Thrown when an {@code If-Match} precondition does not match the current resource version.
 * Mapped to {@code 412 Precondition Failed}.
 */
public class PreconditionFailedException extends RuntimeException {

    public PreconditionFailedException(String message) {
        super(message);
    }
}
