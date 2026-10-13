package com.example.devicesmanager.service;

import com.example.devicesmanager.dto.BrandRequest;
import com.example.devicesmanager.dto.BrandResponse;
import com.example.devicesmanager.dto.PageResponse;
import com.example.devicesmanager.exception.BrandInUseException;
import com.example.devicesmanager.exception.DuplicateBrandNameException;
import com.example.devicesmanager.exception.ResourceNotFoundException;
import com.example.devicesmanager.mapper.BrandMapper;
import com.example.devicesmanager.model.Brand;
import com.example.devicesmanager.repository.BrandRepository;
import com.example.devicesmanager.repository.DeviceRepository;
import com.example.devicesmanager.repository.Pageables;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getBrands(Pageable pageable) {
        return PageResponse.from(brandRepository.findAll(Pageables.withIdTieBreaker(pageable))
                .map(BrandMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(UUID id) {
        return BrandMapper.toResponse(findBrandOrThrow(id));
    }

    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        String name = request.name().trim();
        assertNameAvailable(name, null);

        Brand savedBrand = saveName(Brand.builder().name(name).build());
        return BrandMapper.toResponse(savedBrand);
    }

    @Transactional
    public BrandResponse updateBrand(UUID id, BrandRequest request) {
        Brand brand = findBrandOrThrow(id);
        String name = request.name().trim();
        assertNameAvailable(name, id);

        brand.setName(name);
        return BrandMapper.toResponse(saveName(brand));
    }

    @Transactional
    public void deleteBrand(UUID id) {
        Brand brand = findBrandOrThrow(id);

        if (deviceRepository.existsByBrandId(id)) {
            throw new BrandInUseException("Brand cannot be deleted while devices are associated with it");
        }

        brandRepository.delete(brand);
    }

    private Brand findBrandOrThrow(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    /**
     * Two concurrent requests can both pass {@link #assertNameAvailable} and race on the unique name constraint. The
     * name is the only constraint an insert or rename can violate, so report the loser as a duplicate name too.
     */
    private Brand saveName(Brand brand) {
        try {
            return brandRepository.saveAndFlush(brand);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateBrandNameException("Brand with name '" + brand.getName() + "' already exists");
        }
    }

    private void assertNameAvailable(String name, UUID currentBrandId) {
        brandRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(currentBrandId))
                .ifPresent(existing -> {
                    throw new DuplicateBrandNameException("Brand with name '" + name + "' already exists");
                });
    }
}
