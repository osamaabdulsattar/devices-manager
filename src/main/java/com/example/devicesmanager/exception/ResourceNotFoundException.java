package com.example.devicesmanager.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ProblemType.RESOURCE_NOT_FOUND, message);
    }
}
