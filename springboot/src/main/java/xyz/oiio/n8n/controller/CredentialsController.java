package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.repository.CredentialsRepository;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/rest/credentials")
@RequiredArgsConstructor
public class CredentialsController {

    private final CredentialsRepository credentialsRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCredentials(
            @RequestParam(required = false, defaultValue = "false") boolean includeScopes,
            @RequestParam(required = false, defaultValue = "false") boolean includeData,
            @RequestParam(required = false, defaultValue = "false") boolean includeGlobal,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /credentials - includeScopes: {}, includeData: {}, includeGlobal: {}",
                includeScopes, includeData, includeGlobal);

        // Frontend expects { data: [...] } format
        return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
    }

    @GetMapping("/for-workflow")
    public ResponseEntity<Map<String, Object>> getCredentialsForWorkflow(
            @RequestParam(required = false) String projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /credentials/for-workflow - projectId: {}", projectId);

        // Frontend expects { data: [...] } format
        return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
    }
}
