package com.example.devicesmanager.controller;

import com.example.devicesmanager.constant.ApiConstants;
import com.example.devicesmanager.dto.BrandRequest;
import com.example.devicesmanager.dto.BrandResponse;
import com.example.devicesmanager.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(ApiConstants.BRANDS_PATH)
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<List<BrandResponse>> getBrands() {
        log.debug("GET /api/v1/brands");
        return ResponseEntity.ok(brandService.getBrands());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getBrandById(@PathVariable("id") UUID id) {
        log.debug("GET /api/v1/brands/{}", id);
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @PostMapping
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody BrandRequest request) {
        log.debug("POST /api/v1/brands: {}", request);
        BrandResponse response = brandService.createBrand(request);
        URI location = URI.create(ApiConstants.BRANDS_PATH + "/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable("id") UUID id,
            @Valid @RequestBody BrandRequest request
    ) {
        log.debug("PUT /api/v1/brands/{}: {}", id, request);
        return ResponseEntity.ok(brandService.updateBrand(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable("id") UUID id) {
        log.debug("DELETE /api/v1/brands/{}", id);
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}
