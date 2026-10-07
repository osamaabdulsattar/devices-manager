package com.example.devicesmanager.dto;

import com.example.devicesmanager.model.DeviceState;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DevicePatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "Device name must not be blank")
        @Size(min = 1, max = 150, message = "Device name must be between 1 and 150 characters")
        String name,

        UUID brandId,

        DeviceState state
) {}
