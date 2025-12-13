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
public class ModuleSettingsController {

    @GetMapping("/module-settings")
    public ResponseEntity<Map<String, Object>> getModuleSettings() {
        // Return empty module settings
        return ResponseEntity.ok(Map.of("data", Map.of()));
    }
}
