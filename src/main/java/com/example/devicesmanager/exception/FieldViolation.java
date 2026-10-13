package com.example.devicesmanager.exception;

/**
 * One entry of the {@code errors} extension member of a {@code validation-failed} problem.
 */
public record FieldViolation(
        String field,
        String message
) {}
