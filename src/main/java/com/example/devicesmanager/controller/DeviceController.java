package com.example.devicesmanager.controller;

import com.example.devicesmanager.constant.ApiConstants;
import com.example.devicesmanager.dto.DeviceResponse;
import com.example.devicesmanager.model.DeviceState;
import com.example.devicesmanager.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
