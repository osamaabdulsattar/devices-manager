package com.example.devicesmanager.dto;

import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name
) {}
