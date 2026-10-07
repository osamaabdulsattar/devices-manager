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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private Device iphone;
    private Device macbook;
    private Device galaxy;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
        brandRepository.deleteAll();

        apple = brandRepository.save(Brand.builder().name("Apple").build());
        samsung = brandRepository.save(Brand.builder().name("Samsung").build());

        iphone = deviceRepository.save(Device.builder()
                .name("iPhone 15 Pro")
                .brand(apple)
                .state(DeviceState.AVAILABLE)
                .build());

        macbook = deviceRepository.save(Device.builder()
                .name("MacBook Pro")
                .brand(apple)
                .state(DeviceState.IN_USE)
                .build());

        galaxy = deviceRepository.save(Device.builder()
                .name("Galaxy S24")
                .brand(samsung)
                .state(DeviceState.INACTIVE)
                .build());
    }

    // --- GET /api/v1/devices tests ---

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

    // --- GET /api/v1/devices/{id} tests ---

    @Test
    @DisplayName("GET /api/v1/devices/{id} returns 200 with device details")
    void shouldReturnDeviceById() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(iphone.getId().toString()))
                .andExpect(jsonPath("$.name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.brand.name").value("Apple"))
                .andExpect(jsonPath("$.state").value("available"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/devices/{id} returns 404 when device not found")
    void shouldReturnNotFoundForNonExistentDeviceId() throws Exception {
        mockMvc.perform(get(ApiConstants.DEVICES_PATH + "/" + UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // --- POST /api/v1/devices tests ---

    @Test
    @DisplayName("POST /api/v1/devices creates device and returns 201 with Location header and response body")
    void shouldCreateDeviceSuccessfully() throws Exception {
        String payload = """
                {
                    "name": "iPad Pro",
                    "brandId": "%s",
                    "state": "inactive"
                }
                """.formatted(apple.getId());

        mockMvc.perform(post(ApiConstants.DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(ApiConstants.DEVICES_PATH + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("iPad Pro"))
                .andExpect(jsonPath("$.brand.id").value(apple.getId().toString()))
                .andExpect(jsonPath("$.brand.name").value("Apple"))
                .andExpect(jsonPath("$.state").value("inactive"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/devices defaults state to 'available' when state is omitted")
    void shouldDefaultStateToAvailableOnCreation() throws Exception {
        String payload = """
                {
                    "name": "Apple Watch Ultra",
                    "brandId": "%s"
                }
                """.formatted(apple.getId());

        mockMvc.perform(post(ApiConstants.DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("available"));
    }

    @Test
    @DisplayName("POST /api/v1/devices returns 404 when referenced Brand ID does not exist")
    void shouldReturn404WhenCreatingDeviceWithNonExistentBrand() throws Exception {
        UUID unknownBrandId = UUID.randomUUID();
        String payload = """
                {
                    "name": "Device X",
                    "brandId": "%s"
                }
                """.formatted(unknownBrandId);

        mockMvc.perform(post(ApiConstants.DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/devices returns 400 when name is blank")
    void shouldReturn400WhenDeviceNameIsBlank() throws Exception {
        String payload = """
                {
                    "name": "   ",
                    "brandId": "%s"
                }
                """.formatted(apple.getId());

        mockMvc.perform(post(ApiConstants.DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/devices returns 400 when name exceeds 150 characters")
    void shouldReturn400WhenDeviceNameTooLong() throws Exception {
        String longName = "A".repeat(151);
        String payload = """
                {
                    "name": "%s",
                    "brandId": "%s"
                }
                """.formatted(longName, apple.getId());

        mockMvc.perform(post(ApiConstants.DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/devices returns 400 when state is invalid")
    void shouldReturn400WhenStateIsInvalidOnCreation() throws Exception {
        String payload = """
                {
                    "name": "Device Y",
                    "brandId": "%s",
                    "state": "unknown"
                }
                """.formatted(apple.getId());

        mockMvc.perform(post(ApiConstants.DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- PUT /api/v1/devices/{id} tests ---

    @Test
    @DisplayName("PUT /api/v1/devices/{id} fully updates device and preserves createdAt")
    void shouldFullyUpdateDeviceSuccessfully() throws Exception {
        String originalCreatedAt = iphone.getCreatedAt().toString();
        String payload = """
                {
                    "name": "iPhone 16 Pro Max",
                    "brandId": "%s",
                    "state": "in-use"
                }
                """.formatted(samsung.getId());

        mockMvc.perform(put(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(iphone.getId().toString()))
                .andExpect(jsonPath("$.name").value("iPhone 16 Pro Max"))
                .andExpect(jsonPath("$.brand.id").value(samsung.getId().toString()))
                .andExpect(jsonPath("$.brand.name").value("Samsung"))
                .andExpect(jsonPath("$.state").value("in-use"))
                .andExpect(jsonPath("$.createdAt").value(containsString(originalCreatedAt.substring(0, 19))));
    }

    @Test
    @DisplayName("PUT /api/v1/devices/{id} allows state update on in-use device when name and brand are unchanged")
    void shouldAllowStateUpdateViaPutWhenInUseAndNameBrandUnchanged() throws Exception {
        String payload = """
                {
                    "name": "MacBook Pro",
                    "brandId": "%s",
                    "state": "available"
                }
                """.formatted(apple.getId());

        mockMvc.perform(put(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("available"))
                .andExpect(jsonPath("$.name").value("MacBook Pro"));
    }

    @Test
    @DisplayName("PUT /api/v1/devices/{id} returns 400 when attempting to update name while device is in use")
    void shouldRejectNameUpdateWhenDeviceInUseViaPut() throws Exception {
        String payload = """
                {
                    "name": "MacBook Air M3",
                    "brandId": "%s",
                    "state": "in-use"
                }
                """.formatted(apple.getId());

        mockMvc.perform(put(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("name cannot be updated when device is in use")));
    }

    @Test
    @DisplayName("PUT /api/v1/devices/{id} returns 400 when attempting to update brand while device is in use")
    void shouldRejectBrandUpdateWhenDeviceInUseViaPut() throws Exception {
        String payload = """
                {
                    "name": "MacBook Pro",
                    "brandId": "%s",
                    "state": "in-use"
                }
                """.formatted(samsung.getId());

        mockMvc.perform(put(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("brand cannot be updated when device is in use")));
    }

    @Test
    @DisplayName("PUT /api/v1/devices/{id} returns 404 when device is not found")
    void shouldReturn404WhenUpdatingNonExistentDeviceViaPut() throws Exception {
        String payload = """
                {
                    "name": "Device A",
                    "brandId": "%s",
                    "state": "available"
                }
                """.formatted(apple.getId());

        mockMvc.perform(put(ApiConstants.DEVICES_PATH + "/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/devices/{id} returns 404 when brand is not found")
    void shouldReturn404WhenUpdatingDeviceWithNonExistentBrandViaPut() throws Exception {
        String payload = """
                {
                    "name": "Device A",
                    "brandId": "%s",
                    "state": "available"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(put(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/devices/{id} returns 400 when required fields are missing")
    void shouldReturn400WhenPutMissingFields() throws Exception {
        String payload = """
                {
                    "name": "Device A"
                }
                """;

        mockMvc.perform(put(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- PATCH /api/v1/devices/{id} tests ---

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} updates only name and preserves createdAt")
    void shouldPartiallyUpdateName() throws Exception {
        String originalCreatedAt = iphone.getCreatedAt().toString();
        String payload = """
                {
                    "name": "iPhone 15 Pro Max"
                }
                """;

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone 15 Pro Max"))
                .andExpect(jsonPath("$.brand.name").value("Apple"))
                .andExpect(jsonPath("$.state").value("available"))
                .andExpect(jsonPath("$.createdAt").value(containsString(originalCreatedAt.substring(0, 19))));
    }

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} updates only brand")
    void shouldPartiallyUpdateBrand() throws Exception {
        String payload = """
                {
                    "brandId": "%s"
                }
                """.formatted(samsung.getId());

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand.id").value(samsung.getId().toString()))
                .andExpect(jsonPath("$.brand.name").value("Samsung"))
                .andExpect(jsonPath("$.name").value("iPhone 15 Pro"));
    }

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} updates only state on in-use device")
    void shouldAllowStateUpdateViaPatchWhenInUse() throws Exception {
        String payload = """
                {
                    "state": "inactive"
                }
                """;

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("inactive"))
                .andExpect(jsonPath("$.name").value("MacBook Pro"));
    }

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} returns 400 when attempting to update name while device is in use")
    void shouldRejectNameUpdateWhenDeviceInUseViaPatch() throws Exception {
        String payload = """
                {
                    "name": "MacBook Air"
                }
                """;

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("name cannot be updated when device is in use")));
    }

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} returns 400 when attempting to update brand while device is in use")
    void shouldRejectBrandUpdateWhenDeviceInUseViaPatch() throws Exception {
        String payload = """
                {
                    "brandId": "%s"
                }
                """.formatted(samsung.getId());

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("brand cannot be updated when device is in use")));
    }

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} returns 404 when device not found")
    void shouldReturn404WhenPatchingNonExistentDevice() throws Exception {
        String payload = """
                {
                    "name": "New Name"
                }
                """;

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} returns 404 when referenced brand not found")
    void shouldReturn404WhenPatchingNonExistentBrand() throws Exception {
        String payload = """
                {
                    "brandId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/devices/{id} returns 400 when name is blank")
    void shouldReturn400WhenPatchNameIsBlank() throws Exception {
        String payload = """
                {
                    "name": "   "
                }
                """;

        mockMvc.perform(patch(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // --- DELETE /api/v1/devices/{id} tests ---

    @Test
    @DisplayName("DELETE /api/v1/devices/{id} deletes available device and returns 204")
    void shouldDeleteAvailableDeviceSuccessfully() throws Exception {
        mockMvc.perform(delete(ApiConstants.DEVICES_PATH + "/" + iphone.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiConstants.DEVICES_PATH + "/" + iphone.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/devices/{id} deletes inactive device and returns 204")
    void shouldDeleteInactiveDeviceSuccessfully() throws Exception {
        mockMvc.perform(delete(ApiConstants.DEVICES_PATH + "/" + galaxy.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiConstants.DEVICES_PATH + "/" + galaxy.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/devices/{id} returns 400 when attempting to delete in-use device")
    void shouldRejectDeletionOfInUseDevice() throws Exception {
        mockMvc.perform(delete(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("cannot be deleted while it is in use")));

        // Verify device still exists
        mockMvc.perform(get(ApiConstants.DEVICES_PATH + "/" + macbook.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/devices/{id} returns 404 when device is not found")
    void shouldReturn404WhenDeletingNonExistentDevice() throws Exception {
        mockMvc.perform(delete(ApiConstants.DEVICES_PATH + "/" + UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/v1/devices/{id} returns 400 when ID is malformed UUID")
    void shouldReturn400WhenDeletingWithMalformedUUID() throws Exception {
        mockMvc.perform(delete(ApiConstants.DEVICES_PATH + "/invalid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
