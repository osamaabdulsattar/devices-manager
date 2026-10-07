package com.example.devicesmanager.controller;

import com.example.devicesmanager.constant.ApiConstants;
import com.example.devicesmanager.dto.DeviceCreateRequest;
import com.example.devicesmanager.dto.DevicePatchRequest;
import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.dto.DeviceUpdateRequest;
import com.example.devicesmanager.model.DeviceState;
import com.example.devicesmanager.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<List<DeviceResponse>> getDevices(
            @RequestParam(name = "brandId", required = false) UUID brandId,
            @RequestParam(name = "brandName", required = false) String brandName,
            @RequestParam(name = "state", required = false) DeviceState state
    ) {
        log.debug("GET /api/v1/devices: brandId={}, brandName={}, state={}", brandId, brandName, state);
        List<DeviceResponse> devices = deviceService.getDevices(brandId, brandName, state);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable("id") UUID id) {
        log.debug("GET /api/v1/devices/{}", id);
        DeviceResponse response = deviceService.getDeviceById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        log.debug("POST /api/v1/devices: {}", request);
        DeviceResponse response = deviceService.createDevice(request);
        URI location = URI.create(ApiConstants.DEVICES_PATH + "/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDeviceFully(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DeviceUpdateRequest request
    ) {
        log.debug("PUT /api/v1/devices/{}: {}", id, request);
        DeviceResponse response = deviceService.updateDeviceFully(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevicePartially(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DevicePatchRequest request
    ) {
        log.debug("PATCH /api/v1/devices/{}: {}", id, request);
        DeviceResponse response = deviceService.updateDevicePartially(id, request);
        return ResponseEntity.ok(response);
    }
}
