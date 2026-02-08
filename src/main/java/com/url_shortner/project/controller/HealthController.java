package com.url_shortner.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> healthStatus = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1000)) {
                healthStatus.put("status", "UP");
                healthStatus.put("database", "UP");
                return ResponseEntity.ok(healthStatus);
            } else {
                healthStatus.put("status", "DOWN");
                healthStatus.put("database", "DOWN");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthStatus);
            }
        } catch (Exception e) {
            healthStatus.put("status", "DOWN");
            healthStatus.put("database", "DOWN");
            healthStatus.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthStatus);
        }
    }
}
