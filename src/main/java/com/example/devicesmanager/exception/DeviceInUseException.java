package com.example.devicesmanager.exception;

public class DeviceInUseException extends ApiException {

    public DeviceInUseException(String message) {
        super(ProblemType.DEVICE_IN_USE, message);
    }
}
