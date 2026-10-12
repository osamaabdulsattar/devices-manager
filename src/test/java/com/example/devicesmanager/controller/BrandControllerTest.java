package com.example.devicesmanager.controller;

import com.example.devicesmanager.TestcontainersConfiguration;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class BrandControllerTest {

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
    }

    @Test
    @DisplayName("GET /api/v1/brands returns all brands sorted by name")
    void shouldReturnAllBrands() throws Exception {
        mockMvc.perform(get(ApiConstants.BRANDS_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Apple"))
                .andExpect(jsonPath("$.content[1].name").value("Samsung"));
    }

    @Test
    @DisplayName("GET /api/v1/brands?page&size paginates brands")
    void shouldPaginateBrands() throws Exception {
        mockMvc.perform(get(ApiConstants.BRANDS_PATH)
                        .param("page", "1")
                        .param("size", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Samsung"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/brands/{id} returns the brand")
    void shouldReturnBrandById() throws Exception {
        mockMvc.perform(get(ApiConstants.BRANDS_PATH + "/" + apple.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(apple.getId().toString()))
                .andExpect(jsonPath("$.name").value("Apple"));
    }

    @Test
    @DisplayName("GET /api/v1/brands/{id} returns 404 for unknown brand")
    void shouldReturn404ForUnknownBrand() throws Exception {
        mockMvc.perform(get(ApiConstants.BRANDS_PATH + "/" + UUID.randomUUID()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/brands creates a brand and returns 201 with Location")
    void shouldCreateBrand() throws Exception {
        mockMvc.perform(post(ApiConstants.BRANDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  Google  \"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(ApiConstants.BRANDS_PATH + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Google"));

        assertThat(brandRepository.findByNameIgnoreCase("Google")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/brands returns 409 when the name already exists (case-insensitive)")
    void shouldRejectDuplicateBrandName() throws Exception {
        mockMvc.perform(post(ApiConstants.BRANDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"apple\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/brands returns 400 when the name is blank")
    void shouldRejectBlankBrandName() throws Exception {
        mockMvc.perform(post(ApiConstants.BRANDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("POST /api/v1/brands returns 400 when the name is too long")
    void shouldRejectTooLongBrandName() throws Exception {
        mockMvc.perform(post(ApiConstants.BRANDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "a".repeat(101) + "\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/brands/{id} renames the brand")
    void shouldUpdateBrand() throws Exception {
        mockMvc.perform(put(ApiConstants.BRANDS_PATH + "/" + apple.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Apple Inc.\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apple Inc."));
    }

    @Test
    @DisplayName("PUT /api/v1/brands/{id} allows changing only the casing of the brand's own name")
    void shouldAllowRecasingOwnName() throws Exception {
        mockMvc.perform(put(ApiConstants.BRANDS_PATH + "/" + apple.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"APPLE\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("APPLE"));
    }

    @Test
    @DisplayName("PUT /api/v1/brands/{id} returns 409 when renaming to another brand's name")
    void shouldRejectRenameToExistingName() throws Exception {
        mockMvc.perform(put(ApiConstants.BRANDS_PATH + "/" + apple.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Samsung\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/brands/{id} returns 404 for unknown brand")
    void shouldReturn404WhenUpdatingUnknownBrand() throws Exception {
        mockMvc.perform(put(ApiConstants.BRANDS_PATH + "/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Nokia\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/brands/{id} deletes a brand without devices")
    void shouldDeleteBrandWithoutDevices() throws Exception {
        mockMvc.perform(delete(ApiConstants.BRANDS_PATH + "/" + samsung.getId()))
                .andExpect(status().isNoContent());

        assertThat(brandRepository.findById(samsung.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/brands/{id} returns 409 when devices still reference the brand")
    void shouldRejectDeletingBrandWithDevices() throws Exception {
        mockMvc.perform(delete(ApiConstants.BRANDS_PATH + "/" + apple.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("devices are associated")));

        assertThat(brandRepository.findById(apple.getId())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/brands/{id} returns 404 for unknown brand")
    void shouldReturn404WhenDeletingUnknownBrand() throws Exception {
        mockMvc.perform(delete(ApiConstants.BRANDS_PATH + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
