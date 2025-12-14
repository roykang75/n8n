package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rest")
@RequiredArgsConstructor
public class ProjectsController {

    @GetMapping("/projects/my-projects")
    public ResponseEntity<Map<String, Object>> getMyProjects() {
        // Return empty list for now
        return ResponseEntity.ok(Map.of("data", List.of()));
    }

    @GetMapping("/projects/personal")
    public ResponseEntity<Map<String, Object>> getPersonalProject() {
        // Return a default personal project with team field
        Map<String, Object> project = new HashMap<>();
        project.put("id", "personal");
        project.put("name", "Personal");
        project.put("type", "personal");
        project.put("team", Map.of(
                "id", "personal",
                "name", "Personal",
                "type", "personal"));
        project.put("scopes", List.of(
                "workflow:create",
                "workflow:read",
                "workflow:update",
                "workflow:delete",
                "workflow:execute",
                "workflow:move",
                "workflow:share",
                "credential:create",
                "credential:read",
                "credential:update",
                "credential:delete",
                "credential:move",
                "credential:share",
                "project:read",
                "project:update"));
        return ResponseEntity.ok(Map.of("data", project));
    }

    @GetMapping("/projects/count")
    public ResponseEntity<Map<String, Object>> getProjectsCount() {
        return ResponseEntity.ok(Map.of("data", Map.of("count", 1)));
    }
}
