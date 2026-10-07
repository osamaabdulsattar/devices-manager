package com.example.devicesmanager.dto;

import com.example.devicesmanager.model.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DeviceCreateRequest(
        @NotBlank(message = "Device name must not be blank")
        @Size(min = 1, max = 150, message = "Device name must be between 1 and 150 characters")
        String name,

        @NotNull(message = "Brand ID must not be null")
        UUID brandId,

        DeviceState state
) {
    public DeviceCreateRequest {
        if (state == null) {
            state = DeviceState.AVAILABLE;
        }
    }
}
