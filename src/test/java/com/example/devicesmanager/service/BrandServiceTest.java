package com.example.devicesmanager.service;

import com.example.devicesmanager.dto.BrandRequest;
import com.example.devicesmanager.dto.BrandResponse;
import com.example.devicesmanager.exception.BrandInUseException;
import com.example.devicesmanager.exception.DuplicateBrandNameException;
import com.example.devicesmanager.exception.ResourceNotFoundException;
import com.example.devicesmanager.model.Brand;
import com.example.devicesmanager.repository.BrandRepository;
import com.example.devicesmanager.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    @DisplayName("createBrand trims the name and saves the brand")
    void shouldCreateBrandWithTrimmedName() {
        when(brandRepository.findByNameIgnoreCase("Apple")).thenReturn(Optional.empty());
        when(brandRepository.saveAndFlush(any(Brand.class))).thenAnswer(inv -> {
            Brand brand = inv.getArgument(0);
            brand.setId(UUID.randomUUID());
            return brand;
        });

        BrandResponse response = brandService.createBrand(new BrandRequest("  Apple "));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("createBrand throws DuplicateBrandNameException when the name is taken")
    void shouldRejectDuplicateName() {
        when(brandRepository.findByNameIgnoreCase("Apple"))
                .thenReturn(Optional.of(Brand.builder().id(UUID.randomUUID()).name("Apple").build()));

        assertThatThrownBy(() -> brandService.createBrand(new BrandRequest("Apple")))
                .isInstanceOf(DuplicateBrandNameException.class);

        verify(brandRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createBrand throws DuplicateBrandNameException when a concurrent create wins the unique constraint")
    void shouldMapUniqueConstraintRaceToDuplicateName() {
        when(brandRepository.findByNameIgnoreCase("Apple")).thenReturn(Optional.empty());
        when(brandRepository.saveAndFlush(any(Brand.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> brandService.createBrand(new BrandRequest("Apple")))
                .isInstanceOf(DuplicateBrandNameException.class);
    }

    @Test
    @DisplayName("updateBrand throws ResourceNotFoundException for unknown brand")
    void shouldThrowWhenUpdatingUnknownBrand() {
        UUID id = UUID.randomUUID();
        when(brandRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.updateBrand(id, new BrandRequest("Nokia")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("deleteBrand throws BrandInUseException when devices reference the brand")
    void shouldRejectDeletingBrandInUse() {
        UUID id = UUID.randomUUID();
        Brand brand = Brand.builder().id(id).name("Apple").build();
        when(brandRepository.findById(id)).thenReturn(Optional.of(brand));
        when(deviceRepository.existsByBrandId(id)).thenReturn(true);

        assertThatThrownBy(() -> brandService.deleteBrand(id))
                .isInstanceOf(BrandInUseException.class);

        verify(brandRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteBrand deletes a brand without devices")
    void shouldDeleteUnusedBrand() {
        UUID id = UUID.randomUUID();
        Brand brand = Brand.builder().id(id).name("Apple").build();
        when(brandRepository.findById(id)).thenReturn(Optional.of(brand));
        when(deviceRepository.existsByBrandId(id)).thenReturn(false);

        brandService.deleteBrand(id);

        verify(brandRepository).delete(brand);
    }
}
