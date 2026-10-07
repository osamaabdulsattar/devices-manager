package com.example.devicesmanager.controller;

import com.example.devicesmanager.constant.ApiConstants;
import com.example.devicesmanager.model.Brand;
import com.example.devicesmanager.model.Device;
import com.example.devicesmanager.model.DeviceState;
import com.example.devicesmanager.repository.BrandRepository;
import com.example.devicesmanager.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Brand apple;
    private Brand samsung;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
        brandRepository.deleteAll();

        apple = brandRepository.save(Brand.builder().name("Apple").build());
        samsung = brandRepository.save(Brand.builder().name("Samsung").build());

        deviceRepository.save(Device.builder()
                .name("iPhone 15 Pro")
                .brand(apple)
                .state(DeviceState.AVAILABLE)
                .build());

        deviceRepository.save(Device.builder()
                .name("MacBook Pro")
                .brand(apple)
                .state(DeviceState.IN_USE)
                .build());

        deviceRepository.save(Device.builder()
                .name("Galaxy S24")
                .brand(samsung)
                .state(DeviceState.INACTIVE)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/devices returns 200 with all devices")
    void shouldReturnAllDevices() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("iPhone 15 Pro", "MacBook Pro", "Galaxy S24")))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].brand.id").isNotEmpty())
                .andExpect(jsonPath("$[0].brand.name").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/devices returns empty list when no devices exist")
    void shouldReturnEmptyListWhenNoDevices() throws Exception {
        deviceRepository.deleteAll();

        mockMvc.perform(get(ApiConstants.DEVICES_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/devices?brandId={brandId} filters devices by brand ID")
    void shouldFilterDevicesByBrandId() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH)
                        .param("brandId", apple.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("iPhone 15 Pro", "MacBook Pro")));
    }

    @Test
    @DisplayName("GET /api/v1/devices?brandId={unknown} returns empty list when brand ID has no matches")
    void shouldReturnEmptyListForUnknownBrandId() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH)
                        .param("brandId", UUID.randomUUID().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/devices?brandName={brandName} filters devices by case-insensitive substring")
    void shouldFilterDevicesByBrandNameSubstring() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH)
                        .param("brandName", "app")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("iPhone 15 Pro", "MacBook Pro")));
    }

    @Test
    @DisplayName("GET /api/v1/devices?state={state} filters devices by state")
    void shouldFilterDevicesByState() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH)
                        .param("state", "in-use")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("MacBook Pro"))
                .andExpect(jsonPath("$[0].state").value("in-use"));
    }

    @Test
    @DisplayName("GET /api/v1/devices with combined filters returns matching devices")
    void shouldFilterDevicesWithCombinedCriteria() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH)
                        .param("brandId", apple.getId().toString())
                        .param("state", "available")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$[0].state").value("available"));
    }

    @Test
    @DisplayName("GET /api/v1/devices?state={invalid} returns 400 Bad Request")
    void shouldReturnBadRequestForInvalidState() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH)
                        .param("state", "unknown-state")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/devices?brandId={malformed} returns 400 Bad Request")
    void shouldReturnBadRequestForMalformedUUID() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH)
                        .param("brandId", "not-a-valid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
