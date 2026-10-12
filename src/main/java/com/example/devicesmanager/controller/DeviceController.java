package com.example.devicesmanager.controller;

import com.example.devicesmanager.constant.ApiConstants;
import com.example.devicesmanager.dto.DeviceCreateRequest;
import com.example.devicesmanager.dto.DevicePatchRequest;
import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.dto.DeviceUpdateRequest;
import com.example.devicesmanager.dto.PageResponse;
import com.example.devicesmanager.exception.PreconditionFailedException;
import com.example.devicesmanager.model.DeviceState;
import com.example.devicesmanager.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(ApiConstants.DEVICES_PATH)
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<PageResponse<DeviceResponse>> getDevices(
            @RequestParam(name = "brandId", required = false) UUID brandId,
            @RequestParam(name = "brandName", required = false) String brandName,
            @RequestParam(name = "state", required = false) DeviceState state,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("GET /api/v1/devices: brandId={}, brandName={}, state={}, pageable={}",
                brandId, brandName, state, pageable);
        PageResponse<DeviceResponse> devices = deviceService.getDevices(brandId, brandName, state, pageable);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable("id") UUID id) {
        log.debug("GET /api/v1/devices/{}", id);
        DeviceResponse response = deviceService.getDeviceById(id);
        return ResponseEntity.ok().eTag(eTagOf(response)).body(response);
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        log.debug("POST /api/v1/devices: {}", request);
        DeviceResponse response = deviceService.createDevice(request);
        URI location = URI.create(ApiConstants.DEVICES_PATH + "/" + response.id());
        return ResponseEntity.created(location).eTag(eTagOf(response)).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDeviceFully(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody DeviceUpdateRequest request
    ) {
        log.debug("PUT /api/v1/devices/{}: {}", id, request);
        DeviceResponse response = deviceService.updateDeviceFully(id, request, expectedVersion(ifMatch));
        return ResponseEntity.ok().eTag(eTagOf(response)).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevicePartially(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody DevicePatchRequest request
    ) {
        log.debug("PATCH /api/v1/devices/{}: {}", id, request);
        DeviceResponse response = deviceService.updateDevicePartially(id, request, expectedVersion(ifMatch));
        return ResponseEntity.ok().eTag(eTagOf(response)).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable("id") UUID id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        log.debug("DELETE /api/v1/devices/{}", id);
        deviceService.deleteDevice(id, expectedVersion(ifMatch));
        return ResponseEntity.noContent().build();
    }

    private static String eTagOf(DeviceResponse response) {
        return "\"" + response.version() + "\"";
    }

    /**
     * Extracts the expected entity version from an {@code If-Match} header.
     * Returns {@code null} (no check) when the header is absent or is the {@code *} wildcard.
     */
    private static Long expectedVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        List<ETag> eTags = ETag.parse(ifMatch);
        if (eTags.isEmpty() || eTags.getFirst().isWildcard()) {
            return null;
        }
        try {
            return Long.valueOf(eTags.getFirst().tag());
        } catch (NumberFormatException ex) {
            throw new PreconditionFailedException("If-Match header does not match the current device version");
        }
    }
}
