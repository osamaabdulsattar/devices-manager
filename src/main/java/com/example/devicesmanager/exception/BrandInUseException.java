package com.example.devicesmanager.exception;

public class BrandInUseException extends ApiException {

    public BrandInUseException(String message) {
        super(ProblemType.BRAND_IN_USE, message);
    }
}
