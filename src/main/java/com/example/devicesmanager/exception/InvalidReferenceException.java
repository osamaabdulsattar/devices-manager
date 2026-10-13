package com.example.devicesmanager.exception;

/**
 * Thrown when a request body references a resource that does not exist (e.g. an unknown {@code brandId}).
 * The target URL is valid, so this is mapped to {@code 422 Unprocessable Content} rather than {@code 404}.
 */
public class InvalidReferenceException extends ApiException {

    public InvalidReferenceException(String message) {
        super(ProblemType.INVALID_REFERENCE, message);
    }
}
