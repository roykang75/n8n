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
public class RolesController {

    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getRoles() {
        // Return basic roles
        List<Map<String, Object>> roles = new ArrayList<>();

        Map<String, Object> ownerRole = new HashMap<>();
        ownerRole.put("name", "project:owner");
        ownerRole.put("scope", "project");
        ownerRole.put("licensed", true);
        roles.add(ownerRole);

        Map<String, Object> adminRole = new HashMap<>();
        adminRole.put("name", "project:admin");
        adminRole.put("scope", "project");
        adminRole.put("licensed", true);
        roles.add(adminRole);

        Map<String, Object> editorRole = new HashMap<>();
        editorRole.put("name", "project:editor");
        editorRole.put("scope", "project");
        editorRole.put("licensed", true);
        roles.add(editorRole);

        Map<String, Object> viewerRole = new HashMap<>();
        viewerRole.put("name", "project:viewer");
        viewerRole.put("scope", "project");
        viewerRole.put("licensed", true);
        roles.add(viewerRole);

        return ResponseEntity.ok(Map.of("data", roles));
    }
}
