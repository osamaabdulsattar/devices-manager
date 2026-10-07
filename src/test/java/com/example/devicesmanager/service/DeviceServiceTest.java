package com.example.devicesmanager.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    @DisplayName("getDevices delegates to repository with specifications and maps to DTOs")
    void shouldReturnMappedDeviceResponses() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Instant now = Instant.now();

        Brand brand = Brand.builder()
                .id(brandId)
                .name("Apple")
                .build();

        Device device = Device.builder()
                .id(deviceId)
                .name("iPhone 15 Pro")
                .brand(brand)
                .state(DeviceState.AVAILABLE)
                .createdAt(now)
                .build();

        when(deviceRepository.findAll(any(Specification.class))).thenReturn(List.of(device));

        List<DeviceResponse> results = deviceService.getDevices(brandId, "Apple", DeviceState.AVAILABLE);

        assertThat(results).hasSize(1);
        DeviceResponse response = results.getFirst();
        assertThat(response.id()).isEqualTo(deviceId);
        assertThat(response.name()).isEqualTo("iPhone 15 Pro");
        assertThat(response.brand().id()).isEqualTo(brandId);
        assertThat(response.brand().name()).isEqualTo("Apple");
        assertThat(response.state()).isEqualTo(DeviceState.AVAILABLE);
        assertThat(response.createdAt()).isEqualTo(now);

        verify(deviceRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("getDevices returns empty list when repository returns no devices")
    void shouldReturnEmptyListWhenNoDevicesFound() {
        when(deviceRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<DeviceResponse> results = deviceService.getDevices(null, null, null);

        assertThat(results).isEmpty();
        verify(deviceRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("getDeviceById returns mapped device when found")
    void shouldReturnDeviceById() {
        UUID deviceId = UUID.randomUUID();
        Brand brand = Brand.builder().id(UUID.randomUUID()).name("Apple").build();
        Instant now = Instant.now();
        Device device = Device.builder()
                .id(deviceId)
                .name("iPhone 15 Pro")
                .brand(brand)
                .state(DeviceState.AVAILABLE)
                .createdAt(now)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        DeviceResponse response = deviceService.getDeviceById(deviceId);

        assertThat(response.id()).isEqualTo(deviceId);
        assertThat(response.name()).isEqualTo("iPhone 15 Pro");
        assertThat(response.brand().id()).isEqualTo(brand.getId());
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("getDeviceById throws ResourceNotFoundException when device does not exist")
    void shouldThrowWhenDeviceNotFound() {
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceById(deviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }

    @Test
    @DisplayName("createDevice creates device with specified state")
    void shouldCreateDeviceWithSpecifiedState() {
        UUID brandId = UUID.randomUUID();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();
        Instant now = Instant.now();

        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(deviceRepository.saveAndFlush(any(Device.class))).thenAnswer(invocation -> {
            Device toSave = invocation.getArgument(0);
            return Device.builder()
                    .id(UUID.randomUUID())
                    .name(toSave.getName())
                    .brand(toSave.getBrand())
                    .state(toSave.getState())
                    .createdAt(now)
                    .build();
        });

        DeviceCreateRequest request = new DeviceCreateRequest("iPad Air", brandId, DeviceState.INACTIVE);
        DeviceResponse response = deviceService.createDevice(request);

        assertThat(response.name()).isEqualTo("iPad Air");
        assertThat(response.brand().id()).isEqualTo(brandId);
        assertThat(response.state()).isEqualTo(DeviceState.INACTIVE);
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("createDevice defaults state to AVAILABLE when omitted")
    void shouldDefaultStateToAvailableWhenOmitted() {
        UUID brandId = UUID.randomUUID();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();
        Instant now = Instant.now();

        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(deviceRepository.saveAndFlush(any(Device.class))).thenAnswer(invocation -> {
            Device toSave = invocation.getArgument(0);
            return Device.builder()
                    .id(UUID.randomUUID())
                    .name(toSave.getName())
                    .brand(toSave.getBrand())
                    .state(toSave.getState())
                    .createdAt(now)
                    .build();
        });

        DeviceCreateRequest request = new DeviceCreateRequest("iPad Air", brandId, null);
        DeviceResponse response = deviceService.createDevice(request);

        assertThat(response.state()).isEqualTo(DeviceState.AVAILABLE);
    }

    @Test
    @DisplayName("createDevice throws ResourceNotFoundException when brand does not exist")
    void shouldThrowWhenCreatingDeviceWithUnknownBrand() {
        UUID brandId = UUID.randomUUID();
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        DeviceCreateRequest request = new DeviceCreateRequest("iPad Air", brandId, DeviceState.AVAILABLE);

        assertThatThrownBy(() -> deviceService.createDevice(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(brandId.toString());

        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDeviceFully successfully replaces attributes and preserves createdAt")
    void shouldFullyUpdateDevice() {
        UUID deviceId = UUID.randomUUID();
        UUID oldBrandId = UUID.randomUUID();
        UUID newBrandId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        Brand oldBrand = Brand.builder().id(oldBrandId).name("Apple").build();
        Brand newBrand = Brand.builder().id(newBrandId).name("Google").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("Pixel 7")
                .brand(oldBrand)
                .state(DeviceState.AVAILABLE)
                .createdAt(createdAt)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(newBrandId)).thenReturn(Optional.of(newBrand));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceUpdateRequest request = new DeviceUpdateRequest("Pixel 8 Pro", newBrandId, DeviceState.IN_USE);
        DeviceResponse response = deviceService.updateDeviceFully(deviceId, request);

        assertThat(response.name()).isEqualTo("Pixel 8 Pro");
        assertThat(response.brand().id()).isEqualTo(newBrandId);
        assertThat(response.brand().name()).isEqualTo("Google");
        assertThat(response.state()).isEqualTo(DeviceState.IN_USE);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("updateDeviceFully allows updating state when in-use if name and brand are unchanged")
    void shouldAllowStateUpdateViaPutWhenInUseAndNameBrandUnchanged() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("MacBook Pro")
                .brand(brand)
                .state(DeviceState.IN_USE)
                .createdAt(createdAt)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceUpdateRequest request = new DeviceUpdateRequest("MacBook Pro", brandId, DeviceState.AVAILABLE);
        DeviceResponse response = deviceService.updateDeviceFully(deviceId, request);

        assertThat(response.state()).isEqualTo(DeviceState.AVAILABLE);
        assertThat(response.name()).isEqualTo("MacBook Pro");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("updateDeviceFully throws IllegalArgumentException when updating name of in-use device")
    void shouldRejectNameUpdateWhenDeviceInUseViaPut() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("MacBook Pro")
                .brand(brand)
                .state(DeviceState.IN_USE)
                .createdAt(Instant.now())
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));

        DeviceUpdateRequest request = new DeviceUpdateRequest("MacBook Air", brandId, DeviceState.IN_USE);

        assertThatThrownBy(() -> deviceService.updateDeviceFully(deviceId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be updated when device is in use");

        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDeviceFully throws IllegalArgumentException when updating brand of in-use device")
    void shouldRejectBrandUpdateWhenDeviceInUseViaPut() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UUID otherBrandId = UUID.randomUUID();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();
        Brand otherBrand = Brand.builder().id(otherBrandId).name("Samsung").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("MacBook Pro")
                .brand(brand)
                .state(DeviceState.IN_USE)
                .createdAt(Instant.now())
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(otherBrandId)).thenReturn(Optional.of(otherBrand));

        DeviceUpdateRequest request = new DeviceUpdateRequest("MacBook Pro", otherBrandId, DeviceState.IN_USE);

        assertThatThrownBy(() -> deviceService.updateDeviceFully(deviceId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("brand cannot be updated when device is in use");

        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDeviceFully throws ResourceNotFoundException when device does not exist")
    void shouldThrowWhenFullyUpdatingNonExistentDevice() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        DeviceUpdateRequest request = new DeviceUpdateRequest("Name", brandId, DeviceState.AVAILABLE);

        assertThatThrownBy(() -> deviceService.updateDeviceFully(deviceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }

    @Test
    @DisplayName("updateDeviceFully throws ResourceNotFoundException when brand does not exist")
    void shouldThrowWhenFullyUpdatingWithNonExistentBrand() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Device existing = Device.builder()
                .id(deviceId)
                .name("Name")
                .brand(Brand.builder().id(UUID.randomUUID()).name("Old").build())
                .state(DeviceState.AVAILABLE)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        DeviceUpdateRequest request = new DeviceUpdateRequest("New Name", brandId, DeviceState.AVAILABLE);

        assertThatThrownBy(() -> deviceService.updateDeviceFully(deviceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(brandId.toString());
    }

    @Test
    @DisplayName("updateDevicePartially updates only specified fields and preserves createdAt")
    void shouldPartiallyUpdateDevice() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-02-01T10:00:00Z");
        Brand brand = Brand.builder().id(brandId).name("Apple").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("iPhone 15")
                .brand(brand)
                .state(DeviceState.AVAILABLE)
                .createdAt(createdAt)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        DevicePatchRequest request = new DevicePatchRequest("iPhone 15 Plus", null, null);
        DeviceResponse response = deviceService.updateDevicePartially(deviceId, request);

        assertThat(response.name()).isEqualTo("iPhone 15 Plus");
        assertThat(response.brand().id()).isEqualTo(brandId);
        assertThat(response.state()).isEqualTo(DeviceState.AVAILABLE);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("updateDevicePartially allows updating state on in-use device")
    void shouldAllowStateUpdateViaPatchWhenInUse() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("MacBook Pro")
                .brand(brand)
                .state(DeviceState.IN_USE)
                .createdAt(createdAt)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        DevicePatchRequest request = new DevicePatchRequest(null, null, DeviceState.INACTIVE);
        DeviceResponse response = deviceService.updateDevicePartially(deviceId, request);

        assertThat(response.state()).isEqualTo(DeviceState.INACTIVE);
        assertThat(response.name()).isEqualTo("MacBook Pro");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("updateDevicePartially throws IllegalArgumentException when updating name of in-use device")
    void shouldRejectNameUpdateWhenDeviceInUseViaPatch() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("MacBook Pro")
                .brand(brand)
                .state(DeviceState.IN_USE)
                .createdAt(Instant.now())
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));

        DevicePatchRequest request = new DevicePatchRequest("MacBook Air", null, null);

        assertThatThrownBy(() -> deviceService.updateDevicePartially(deviceId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be updated when device is in use");

        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDevicePartially throws IllegalArgumentException when updating brand of in-use device")
    void shouldRejectBrandUpdateWhenDeviceInUseViaPatch() {
        UUID deviceId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UUID otherBrandId = UUID.randomUUID();
        Brand brand = Brand.builder().id(brandId).name("Apple").build();
        Brand otherBrand = Brand.builder().id(otherBrandId).name("Samsung").build();

        Device existing = Device.builder()
                .id(deviceId)
                .name("MacBook Pro")
                .brand(brand)
                .state(DeviceState.IN_USE)
                .createdAt(Instant.now())
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(otherBrandId)).thenReturn(Optional.of(otherBrand));

        DevicePatchRequest request = new DevicePatchRequest(null, otherBrandId, null);

        assertThatThrownBy(() -> deviceService.updateDevicePartially(deviceId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("brand cannot be updated when device is in use");

        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDevicePartially throws IllegalArgumentException when name is blank")
    void shouldRejectBlankNameInPatch() {
        UUID deviceId = UUID.randomUUID();
        Device existing = Device.builder()
                .id(deviceId)
                .name("Phone")
                .brand(Brand.builder().id(UUID.randomUUID()).name("Apple").build())
                .state(DeviceState.AVAILABLE)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));

        DevicePatchRequest request = new DevicePatchRequest("   ", null, null);

        assertThatThrownBy(() -> deviceService.updateDevicePartially(deviceId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("deleteDevice deletes device when state is not in-use")
    void shouldDeleteDeviceSuccessfully() {
        UUID deviceId = UUID.randomUUID();
        Device existing = Device.builder()
                .id(deviceId)
                .name("Phone")
                .brand(Brand.builder().id(UUID.randomUUID()).name("Apple").build())
                .state(DeviceState.AVAILABLE)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));

        deviceService.deleteDevice(deviceId);

        verify(deviceRepository, times(1)).delete(existing);
    }

    @Test
    @DisplayName("deleteDevice throws IllegalArgumentException when device is in use")
    void shouldThrowWhenDeletingInUseDevice() {
        UUID deviceId = UUID.randomUUID();
        Device existing = Device.builder()
                .id(deviceId)
                .name("MacBook Pro")
                .brand(Brand.builder().id(UUID.randomUUID()).name("Apple").build())
                .state(DeviceState.IN_USE)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> deviceService.deleteDevice(deviceId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be deleted while it is in use");

        verify(deviceRepository, never()).delete(any(Device.class));
    }

    @Test
    @DisplayName("deleteDevice throws ResourceNotFoundException when device does not exist")
    void shouldThrowWhenDeletingNonExistentDevice() {
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.deleteDevice(deviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());

        verify(deviceRepository, never()).delete(any(Device.class));
    }
}
