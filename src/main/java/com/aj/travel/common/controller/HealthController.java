package com.aj.travel.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> apiInfo() {
        return Map.of(
                "health", "/api/system/health",
                "message", "TripOps | Booking & Inventory Management System API is running",
                "swagger", "/swagger-ui/index.html"
        );
    }
}