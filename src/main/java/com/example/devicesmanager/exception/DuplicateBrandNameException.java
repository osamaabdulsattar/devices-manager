package com.example.devicesmanager.exception;

public class DuplicateBrandNameException extends ApiException {

    public DuplicateBrandNameException(String message) {
        super(ProblemType.DUPLICATE_BRAND_NAME, message);
    }
}
