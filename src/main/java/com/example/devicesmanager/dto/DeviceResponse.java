package com.example.devicesmanager.dto;

import com.example.devicesmanager.model.DeviceState;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String name,
        BrandResponse brand,
        DeviceState state,
        Instant createdAt
) {}
