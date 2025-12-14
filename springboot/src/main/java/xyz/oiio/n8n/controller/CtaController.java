package xyz.oiio.n8n.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rest/cta")
public class CtaController {

    @GetMapping("/become-creator")
    public ResponseEntity<Map<String, Object>> getBecomeCreator() {
        log.info("GET /cta/become-creator");

        // Return empty response for now
        Map<String, Object> response = Map.of(
                "data", Map.of(
                        "show", false));
        return ResponseEntity.ok(response);
    }
}
