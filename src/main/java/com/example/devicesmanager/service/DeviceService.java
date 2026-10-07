package com.example.devicesmanager.service;

import com.example.devicesmanager.dto.BrandResponse;
import com.example.devicesmanager.dto.DeviceCreateRequest;
import com.example.devicesmanager.dto.DevicePatchRequest;
import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.dto.DeviceUpdateRequest;
import com.example.devicesmanager.exception.ResourceNotFoundException;
import com.example.devicesmanager.model.Brand;
import com.example.devicesmanager.model.Device;
import com.example.devicesmanager.model.DeviceState;
import com.example.devicesmanager.repository.BrandRepository;
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
    private final BrandRepository brandRepository;

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

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(UUID id) {
        Device device = findDeviceOrThrow(id);
        return toResponse(device);
    }

    @Transactional
    public DeviceResponse createDevice(DeviceCreateRequest request) {
        Brand brand = findBrandOrThrow(request.brandId());

        Device device = Device.builder()
                .name(request.name())
                .brand(brand)
                .state(request.state() != null ? request.state() : DeviceState.AVAILABLE)
                .build();

        Device savedDevice = deviceRepository.saveAndFlush(device);
        return toResponse(savedDevice);
    }

    @Transactional
    public DeviceResponse updateDeviceFully(UUID id, DeviceUpdateRequest request) {
        Device device = findDeviceOrThrow(id);
        Brand brand = findBrandOrThrow(request.brandId());

        assertMutableWhileInUse(device, request.name(), request.brandId());

        device.setName(request.name());
        device.setBrand(brand);
        device.setState(request.state());

        Device updatedDevice = deviceRepository.save(device);
        return toResponse(updatedDevice);
    }

    @Transactional
    public DeviceResponse updateDevicePartially(UUID id, DevicePatchRequest request) {
        Device device = findDeviceOrThrow(id);

        Brand brand = null;
        if (request.brandId() != null) {
            brand = findBrandOrThrow(request.brandId());
        }

        assertMutableWhileInUse(device, request.name(), request.brandId());

        if (request.name() != null) {
            if (request.name().trim().isEmpty()) {
                throw new IllegalArgumentException("Device name must not be blank");
            }
            device.setName(request.name());
        }

        if (brand != null) {
            device.setBrand(brand);
        }

        if (request.state() != null) {
            device.setState(request.state());
        }

        Device updatedDevice = deviceRepository.save(device);
        return toResponse(updatedDevice);
    }

    @Transactional
    public void deleteDevice(UUID id) {
        Device device = findDeviceOrThrow(id);

        if (device.getState() == DeviceState.IN_USE) {
            throw new IllegalArgumentException("Device cannot be deleted while it is in use");
        }

        deviceRepository.delete(device);
    }

    private Device findDeviceOrThrow(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + id));
    }

    private Brand findBrandOrThrow(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
    }

    private void assertMutableWhileInUse(Device device, String newName, UUID newBrandId) {
        if (device.getState() != DeviceState.IN_USE) {
            return;
        }
        if (newName != null && !device.getName().equals(newName)) {
            throw new IllegalArgumentException("Device name cannot be updated when device is in use");
        }
        if (newBrandId != null && !device.getBrand().getId().equals(newBrandId)) {
            throw new IllegalArgumentException("Device brand cannot be updated when device is in use");
        }
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
