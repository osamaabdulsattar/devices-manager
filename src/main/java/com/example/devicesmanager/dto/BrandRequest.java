package com.example.devicesmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank(message = "Brand name must not be blank")
        @Size(max = 100, message = "Brand name must be at most 100 characters")
        String name
) {}
