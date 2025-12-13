package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/rest")
@RequiredArgsConstructor
public class LicenseController {

    @GetMapping("/license")
    public ResponseEntity<Map<String, Object>> getLicense() {
        Map<String, Object> license = new HashMap<>();
        license.put("usage", Map.of(
                "executions", Map.of("value", 0, "limit", -1, "warningThreshold", 0.8),
                "activeWorkflows", Map.of("value", 0, "limit", -1, "warningThreshold", 0.8)));
        license.put("license", Map.of(
                "planId", "",
                "planName", "Community"));
        return ResponseEntity.ok(Map.of("data", license));
    }
}
