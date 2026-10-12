package com.example.devicesmanager.mapper;

import com.example.devicesmanager.dto.BrandResponse;
import com.example.devicesmanager.model.Brand;

public final class BrandMapper {

    private BrandMapper() {}

    public static BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName());
    }
}
