package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.entity.WorkflowEntity;
import xyz.oiio.n8n.service.workflow.WorkflowService;
import xyz.oiio.n8n.service.user.UserService;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/rest")
@RequiredArgsConstructor
public class ActiveWorkflowsController {

    private final WorkflowService workflowService;
    private final UserService userService;

    @GetMapping("/active-workflows")
    public ResponseEntity<Map<String, Object>> getActiveWorkflows(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.ok(Map.of("data", List.of()));
            }

            xyz.oiio.n8n.entity.User currentUser = userService.getUserById(authentication.getName());
            List<WorkflowEntity> workflows = workflowService.getWorkflowsByUser(currentUser).stream()
                    .filter(w -> w.getActive() != null && w.getActive())
                    .collect(Collectors.toList());

            List<String> activeWorkflowIds = workflows.stream()
                    .map(WorkflowEntity::getId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("data", activeWorkflowIds));
        } catch (Exception e) {
            log.error("Error fetching active workflows", e);
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
    }
}
