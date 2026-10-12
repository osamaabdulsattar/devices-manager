package com.example.devicesmanager.mapper;

import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.model.Device;

public final class DeviceMapper {

    private DeviceMapper() {}

    public static DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                BrandMapper.toResponse(device.getBrand()),
                device.getState(),
                device.getCreatedAt(),
                device.getVersion()
        );
    }
}
