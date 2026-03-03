package com.limitr.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoApiController {

    @GetMapping("/public/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "message", "pong",
            "timestamp", Instant.now()
        );
    }

    @GetMapping("/data")
    public Map<String, Object> data() {
        return Map.of(
            "service", "Limitr Demo API",
            "status", "ok",
            "timestamp", Instant.now(),
            "data", Map.of("sensitive", false, "sample", "secure-data-stream")
        );
    }

    @GetMapping("/resource/{id}")
    public Map<String, Object> resource(@PathVariable Long id) {
        return Map.of(
            "id", id,
            "name", "Resource-" + id,
            "timestamp", Instant.now()
        );
    }
}
