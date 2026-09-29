package com.codereviewagent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> getHome() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Code Review Agent API");
        response.put("version", "1.0.0");
        response.put("status", "RUNNING");
        response.put("message", "Welcome to Code Review Agent Backend!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "PostgreSQL (codereview_agent)");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
