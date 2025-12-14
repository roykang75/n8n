package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.entity.CredentialsEntity;
import xyz.oiio.n8n.repository.CredentialsRepository;
import xyz.oiio.n8n.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/rest/credentials")
@RequiredArgsConstructor
public class CredentialsController {

    private final CredentialsRepository credentialsRepository;
    private final UserRepository userRepository;

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

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCredential(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /credentials - request: {}", request);

        String name = (String) request.get("name");
        String type = (String) request.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        // Create credential entity - let JPA auto-generate ID and timestamps
        CredentialsEntity credential = CredentialsEntity.builder()
                .name(name)
                .type(type)
                .data(data != null ? data.toString() : "{}")
                .isManaged(false)
                .isGlobal(false)
                .isResolvable(false)
                .resolvableAllowFallback(false)
                .build();

        // Save credential
        CredentialsEntity saved = credentialsRepository.save(credential);

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("type", saved.getType());
        response.put("createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null);
        response.put("updatedAt", saved.getUpdatedAt() != null ? saved.getUpdatedAt().toString() : null);
        response.put("isManaged", false);

        // Frontend expects { data: {...} } format
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCredential(
            @PathVariable String id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("PATCH /credentials/{} - request: {}", id, request);

        // For now, return a basic response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        response.put("name", request.get("name"));
        response.put("type", request.get("type"));
        response.put("updatedAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(Map.of("data", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCredential(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DELETE /credentials/{}", id);

        credentialsRepository.deleteById(id);

        return ResponseEntity.ok(Map.of("data", Map.of("success", true)));
    }
}
