package com.ticketrush.controller;

import com.ticketrush.service.ReadinessState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final ReadinessState readiness;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        if (!readiness.isReady()) {
            return ResponseEntity.status(503).body(Map.of("status", "STARTING"));
        }
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
