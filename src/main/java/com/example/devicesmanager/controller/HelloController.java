package com.example.devicesmanager.controller;

import com.example.devicesmanager.dto.HelloResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public ResponseEntity<HelloResponse> hello(
            @RequestParam(name = "name", defaultValue = "World") String name) {
        log.info("Greeting requested for name: {}", name);
        HelloResponse response = HelloResponse.builder()
                .message("Hello, " + name + "!")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
