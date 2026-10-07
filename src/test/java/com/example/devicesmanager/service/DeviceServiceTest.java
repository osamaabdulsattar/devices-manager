package com.example.devicesmanager.service;

import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.model.Brand;
import com.example.devicesmanager.model.Device;
import com.example.devicesmanager.model.DeviceState;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

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
}
