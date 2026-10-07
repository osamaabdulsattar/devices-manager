package com.example.devicesmanager.dto;

public record FieldErrorResponse(
        String field,
        String message
) {}
