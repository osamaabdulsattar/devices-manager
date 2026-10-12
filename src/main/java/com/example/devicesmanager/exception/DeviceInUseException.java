package com.example.devicesmanager.exception;

public class DeviceInUseException extends ConflictException {

    public DeviceInUseException(String message) {
        super(message);
    }
}
