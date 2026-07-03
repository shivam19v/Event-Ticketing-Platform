package com.eventsphere.notification.controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class HealthController {
    @GetMapping("/api/v1/notifications/ping")
    public Map<String, String> ping() { return Map.of("status", "notification-service is running"); }
}
