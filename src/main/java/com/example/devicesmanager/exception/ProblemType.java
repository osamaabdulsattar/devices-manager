package com.example.devicesmanager.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Application-specific RFC 9457 problem types. Each one is identified by a stable {@code type} URI that clients can
 * switch on instead of parsing the human-readable {@code detail}. Generic HTTP errors raised by the framework
 * (405, 406, 415, unknown paths) keep the standard {@code about:blank} type.
 */
public enum ProblemType {

    RESOURCE_NOT_FOUND("resource-not-found", "Resource not found", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED("validation-failed", "Validation failed", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("malformed-request", "Malformed request body", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("invalid-parameter", "Invalid request parameter", HttpStatus.BAD_REQUEST),
    INVALID_REFERENCE("invalid-reference", "Referenced resource does not exist", HttpStatus.UNPROCESSABLE_CONTENT),
    DEVICE_IN_USE("device-in-use", "Device is in use", HttpStatus.CONFLICT),
    BRAND_IN_USE("brand-in-use", "Brand is in use", HttpStatus.CONFLICT),
    DUPLICATE_BRAND_NAME("duplicate-brand-name", "Brand name already exists", HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION("concurrent-modification", "Concurrent modification", HttpStatus.CONFLICT),
    DATA_CONFLICT("data-conflict", "Data conflict", HttpStatus.CONFLICT),
    PRECONDITION_FAILED("precondition-failed", "Precondition failed", HttpStatus.PRECONDITION_FAILED),
    INTERNAL_ERROR("internal-error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private static final String BASE_URI = "/problems/";

    private final String slug;
    private final String title;
    private final HttpStatus status;

    ProblemType(String slug, String title, HttpStatus status) {
        this.slug = slug;
        this.title = title;
        this.status = status;
    }

    public URI uri() {
        return URI.create(BASE_URI + slug);
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }
}
