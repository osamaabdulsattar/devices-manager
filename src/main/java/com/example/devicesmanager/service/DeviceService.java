package com.example.devicesmanager.service;

import com.example.devicesmanager.dto.BrandResponse;
import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.model.Device;
import com.example.devicesmanager.model.DeviceState;
import com.example.devicesmanager.repository.DeviceRepository;
import com.example.devicesmanager.repository.DeviceSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices(UUID brandId, String brandName, DeviceState state) {
        Specification<Device> spec = Specification
                .where(DeviceSpecifications.hasBrandId(brandId))
                .and(DeviceSpecifications.hasBrandNameLike(brandName))
                .and(DeviceSpecifications.hasState(state));

        return deviceRepository.findAll(spec)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                new BrandResponse(device.getBrand().getId(), device.getBrand().getName()),
                device.getState(),
                device.getCreatedAt()
        );
    }
}
