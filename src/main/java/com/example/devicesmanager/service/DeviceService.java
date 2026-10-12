package com.example.devicesmanager.service;

import com.example.devicesmanager.dto.DeviceCreateRequest;
import com.example.devicesmanager.dto.DevicePatchRequest;
import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.dto.DeviceUpdateRequest;
import com.example.devicesmanager.dto.PageResponse;
import com.example.devicesmanager.exception.DeviceInUseException;
import com.example.devicesmanager.exception.InvalidReferenceException;
import com.example.devicesmanager.exception.PreconditionFailedException;
import com.example.devicesmanager.exception.ResourceNotFoundException;
import com.example.devicesmanager.mapper.DeviceMapper;
import com.example.devicesmanager.model.Brand;
import com.example.devicesmanager.model.Device;
import com.example.devicesmanager.model.DeviceState;
import com.example.devicesmanager.repository.BrandRepository;
import com.example.devicesmanager.repository.DeviceRepository;
import com.example.devicesmanager.repository.DeviceSpecifications;
import com.example.devicesmanager.repository.Pageables;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public PageResponse<DeviceResponse> getDevices(UUID brandId, String brandName, DeviceState state, Pageable pageable) {
        Specification<Device> spec = Specification
                .where(DeviceSpecifications.hasBrandId(brandId))
                .and(DeviceSpecifications.hasBrandNameLike(brandName))
                .and(DeviceSpecifications.hasState(state));

        return PageResponse.from(deviceRepository.findAll(spec, Pageables.withIdTieBreaker(pageable))
                .map(DeviceMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(UUID id) {
        Device device = findDeviceOrThrow(id);
        return DeviceMapper.toResponse(device);
    }

    @Transactional
    public DeviceResponse createDevice(DeviceCreateRequest request) {
        Brand brand = findReferencedBrand(request.brandId());

        Device device = Device.builder()
                .name(request.name())
                .brand(brand)
                .state(request.state() != null ? request.state() : DeviceState.AVAILABLE)
                .build();

        Device savedDevice = deviceRepository.saveAndFlush(device);
        return DeviceMapper.toResponse(savedDevice);
    }

    /**
     * @param expectedVersion version from the client's {@code If-Match} header, or {@code null} to skip the check
     */
    @Transactional
    public DeviceResponse updateDeviceFully(UUID id, DeviceUpdateRequest request, Long expectedVersion) {
        Device device = findDeviceOrThrow(id);
        assertVersionMatches(device, expectedVersion);
        Brand brand = findReferencedBrand(request.brandId());

        assertMutableWhileInUse(device, request.name(), request.brandId());

        device.setName(request.name());
        device.setBrand(brand);
        device.setState(request.state());

        // Flush so the incremented version is reflected in the response (and its ETag).
        Device updatedDevice = deviceRepository.saveAndFlush(device);
        return DeviceMapper.toResponse(updatedDevice);
    }

    /**
     * @param expectedVersion version from the client's {@code If-Match} header, or {@code null} to skip the check
     */
    @Transactional
    public DeviceResponse updateDevicePartially(UUID id, DevicePatchRequest request, Long expectedVersion) {
        Device device = findDeviceOrThrow(id);
        assertVersionMatches(device, expectedVersion);

        Brand brand = null;
        if (request.brandId() != null) {
            brand = findReferencedBrand(request.brandId());
        }

        assertMutableWhileInUse(device, request.name(), request.brandId());

        if (request.name() != null) {
            device.setName(request.name());
        }

        if (brand != null) {
            device.setBrand(brand);
        }

        if (request.state() != null) {
            device.setState(request.state());
        }

        Device updatedDevice = deviceRepository.saveAndFlush(device);
        return DeviceMapper.toResponse(updatedDevice);
    }

    /**
     * @param expectedVersion version from the client's {@code If-Match} header, or {@code null} to skip the check
     */
    @Transactional
    public void deleteDevice(UUID id, Long expectedVersion) {
        Device device = findDeviceOrThrow(id);
        assertVersionMatches(device, expectedVersion);

        if (device.getState() == DeviceState.IN_USE) {
            throw new DeviceInUseException("Device cannot be deleted while it is in use");
        }

        deviceRepository.delete(device);
    }

    private Device findDeviceOrThrow(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + id));
    }

    private Brand findReferencedBrand(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new InvalidReferenceException("Brand not found with id: " + brandId));
    }

    private void assertVersionMatches(Device device, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(device.getVersion())) {
            throw new PreconditionFailedException(
                    "Device has been modified (current version " + device.getVersion() + "); fetch it again and retry");
        }
    }

    private void assertMutableWhileInUse(Device device, String newName, UUID newBrandId) {
        if (device.getState() != DeviceState.IN_USE) {
            return;
        }
        if (newName != null && !device.getName().equals(newName)) {
            throw new DeviceInUseException("Device name cannot be updated when device is in use");
        }
        if (newBrandId != null && !device.getBrand().getId().equals(newBrandId)) {
            throw new DeviceInUseException("Device brand cannot be updated when device is in use");
        }
    }
}
