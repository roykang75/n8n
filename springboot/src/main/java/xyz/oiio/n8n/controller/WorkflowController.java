package xyz.oiio.n8n.controller;

import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.entity.WorkflowEntity;
import xyz.oiio.n8n.service.workflow.WorkflowService;
import xyz.oiio.n8n.service.user.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/rest/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final UserService userService;

    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("data", Map.of(), "message", "Authentication required"));
            }

            xyz.oiio.n8n.entity.User currentUser = userService.getUserById(authentication.getName());
            WorkflowEntity createdWorkflow = workflowService.createWorkflow(request.toServiceRequest(objectMapper),
                    currentUser);

            Map<String, Object> workflowData = workflowToMap(createdWorkflow);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", workflowData));
        } catch (Exception e) {
            log.error("Error creating workflow: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("data", Map.of(), "message", e.getMessage()));
        }
    }

    @GetMapping("/{workflowId}")
    @PreAuthorize("hasRole('ADMIN') or @workflowService.isOwner(#workflowId, authentication.name)")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable String workflowId) {
        try {
            WorkflowEntity workflow = workflowService.getWorkflowById(workflowId);
            return ResponseEntity
                    .ok(new WorkflowResponse(true, "Workflow retrieved successfully", new WorkflowDto(workflow)));
        } catch (WorkflowService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWorkflows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            Authentication authentication) {
        try {
            // Handle unauthenticated requests
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.ok(Map.of("data", List.of(), "count", 0));
            }

            xyz.oiio.n8n.entity.User currentUser = userService.getUserById(authentication.getName());
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
            Page<WorkflowEntity> workflows = workflowService.getWorkflowsByUser(currentUser, pageable);

            List<Map<String, Object>> workflowList = workflows.getContent().stream()
                    .map(this::workflowToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "data", workflowList,
                    "count", workflows.getTotalElements()));
        } catch (Exception e) {
            // Return empty list on error instead of 500
            return ResponseEntity.ok(Map.of("data", List.of(), "count", 0));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveWorkflows(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.ok(Map.of("data", List.of()));
            }

            xyz.oiio.n8n.entity.User currentUser = userService.getUserById(authentication.getName());
            List<WorkflowEntity> workflows = workflowService.getWorkflowsByUser(currentUser).stream()
                    .filter(w -> w.getActive() != null && w.getActive())
                    .collect(Collectors.toList());

            List<Map<String, Object>> workflowList = workflows.stream()
                    .map(this::workflowToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("data", workflowList));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
    }

    @GetMapping("/new")
    public ResponseEntity<Map<String, String>> getNewWorkflowName() {
        System.out.println("CHECK SERVER VERSION: 3 (System.out)");
        log.info("CHECK SERVER VERSION: 3");
        // Generate default workflow name
        return ResponseEntity.ok(Map.of("name", "My workflow"));
    }

    private Map<String, Object> workflowToMap(WorkflowEntity workflow) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", workflow.getId());
        map.put("name", workflow.getName());
        map.put("active", workflow.getActive());
        map.put("createdAt", workflow.getCreatedAt() != null ? workflow.getCreatedAt().toString() : null);
        map.put("updatedAt", workflow.getUpdatedAt() != null ? workflow.getUpdatedAt().toString() : null);
        map.put("nodes", workflow.getNodes() != null ? workflow.getNodes() : List.of());
        map.put("connections", workflow.getConnections() != null ? workflow.getConnections() : Map.of());
        map.put("versionId", workflow.getVersionId());
        map.put("tags",
                workflow.getTags() != null
                        ? workflow.getTags().stream().map(t -> Map.of("id", t.getId(), "name", t.getName()))
                                .collect(Collectors.toList())
                        : List.of());
        return map;
    }

    @GetMapping("/all-accessible")
    public ResponseEntity<WorkflowsResponse> getAccessibleWorkflows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<WorkflowEntity> workflows = workflowService.getAccessibleWorkflows(authentication.getName(), pageable);

        return ResponseEntity.ok(WorkflowsResponse.builder()
                .success(true)
                .message("Workflows retrieved successfully")
                .workflows(workflows.getContent().stream()
                        .map(WorkflowDto::new)
                        .collect(Collectors.toList()))
                .pageInfo(new PageInfo(workflows))
                .build());
    }

    @PutMapping("/{workflowId}")
    @PreAuthorize("hasRole('ADMIN') or @workflowService.isOwner(#workflowId, authentication.name)")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable String workflowId,
            @Valid @RequestBody UpdateWorkflowRequest request) {
        try {
            WorkflowEntity updatedWorkflow = workflowService.updateWorkflow(workflowId,
                    request.toServiceRequest(objectMapper));
            return ResponseEntity
                    .ok(new WorkflowResponse(true, "Workflow updated successfully", new WorkflowDto(updatedWorkflow)));
        } catch (WorkflowService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{workflowId}")
    @PreAuthorize("hasRole('ADMIN') or @workflowService.isOwner(#workflowId, authentication.name)")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String workflowId) {
        try {
            workflowService.deleteWorkflow(workflowId);
            return ResponseEntity.noContent().build();
        } catch (WorkflowService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{workflowId}/activate")
    @PreAuthorize("hasRole('ADMIN') or @workflowService.isOwner(#workflowId, authentication.name)")
    public ResponseEntity<WorkflowResponse> activateWorkflow(@PathVariable String workflowId) {
        try {
            WorkflowEntity workflow = workflowService.activateWorkflow(workflowId);
            return ResponseEntity.ok(WorkflowResponse.builder()
                    .success(true)
                    .message("Workflow activated successfully")
                    .workflow(new WorkflowDto(workflow))
                    .build());
        } catch (WorkflowService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{workflowId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or @workflowService.isOwner(#workflowId, authentication.name)")
    public ResponseEntity<WorkflowResponse> deactivateWorkflow(@PathVariable String workflowId) {
        try {
            WorkflowEntity workflow = workflowService.deactivateWorkflow(workflowId);
            return ResponseEntity.ok(WorkflowResponse.builder()
                    .success(true)
                    .message("Workflow deactivated successfully")
                    .workflow(new WorkflowDto(workflow))
                    .build());
        } catch (WorkflowService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tag/{tagName}")
    public ResponseEntity<WorkflowsResponse> getWorkflowsByTag(@PathVariable String tagName,
            Authentication authentication) {
        try {
            xyz.oiio.n8n.entity.User currentUser = userService.getUserById(authentication.getName());
            List<WorkflowEntity> workflows = workflowService.getWorkflowsByUser(currentUser);

            // 태그 필터링
            List<WorkflowEntity> filteredWorkflows = workflows.stream()
                    .filter(workflow -> workflow.getTags() != null)
                    .filter(workflow -> workflow.getTags().stream()
                            .anyMatch(tag -> tag.getName().equals(tagName)))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(WorkflowsResponse.builder()
                    .success(true)
                    .workflows(filteredWorkflows.stream()
                            .map(WorkflowDto::new)
                            .collect(Collectors.toList()))
                    .pageInfo(null) // 리스트이므로 페이지 정보 없음
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(WorkflowsResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .workflows(List.of())
                            .pageInfo(null)
                            .build());
        }
    }

    // DTOs
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateWorkflowRequest {
        private String name;
        private String description;
        private List<Map<String, Object>> nodes;
        private Map<String, Object> connections;
        private Object settings;
        private Map<String, Object> staticData;
        private Object meta;
        private Map<String, Object> pinData;
        private String projectId;

        @JsonProperty("tags")
        private List<Object> tags = List.of();

        public WorkflowService.WorkflowRequest toServiceRequest(ObjectMapper mapper) {
            List<String> tagNamesList = tags.stream()
                    .map(t -> {
                        if (t instanceof Map) {
                            return (String) ((Map<?, ?>) t).get("name");
                        }
                        return t.toString();
                    })
                    .collect(Collectors.toList());

            // settings와 meta 변환 (Injected ObjectMapper 사용)
            WorkflowEntity.WorkflowSettings settingsObj = settings != null
                    ? mapper.convertValue(settings, WorkflowEntity.WorkflowSettings.class)
                    : new WorkflowEntity.WorkflowSettings();

            WorkflowEntity.WorkflowMeta metaObj = meta != null
                    ? mapper.convertValue(meta, WorkflowEntity.WorkflowMeta.class)
                    : new WorkflowEntity.WorkflowMeta();

            return new WorkflowService.WorkflowRequest(
                    name, description, nodes, connections, settingsObj, staticData,
                    metaObj, pinData, Optional.ofNullable(projectId), tagNamesList);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateWorkflowRequest {
        private String name;
        private String description;
        private List<Map<String, Object>> nodes;
        private Map<String, Object> connections;
        private Object settings;
        private Map<String, Object> staticData;
        private Object meta;
        private Map<String, Object> pinData;
        private String projectId;

        @JsonProperty("tags")
        private List<Object> tags = List.of();

        public WorkflowService.WorkflowRequest toServiceRequest(ObjectMapper mapper) {
            List<String> tagNamesList = tags.stream()
                    .map(t -> {
                        if (t instanceof Map) {
                            return (String) ((Map<?, ?>) t).get("name");
                        }
                        return t.toString();
                    })
                    .collect(Collectors.toList());

            // settings와 meta 변환 (Injected ObjectMapper 사용)
            WorkflowEntity.WorkflowSettings settingsObj = settings != null
                    ? mapper.convertValue(settings, WorkflowEntity.WorkflowSettings.class)
                    : new WorkflowEntity.WorkflowSettings();

            WorkflowEntity.WorkflowMeta metaObj = meta != null
                    ? mapper.convertValue(meta, WorkflowEntity.WorkflowMeta.class)
                    : new WorkflowEntity.WorkflowMeta();

            return new WorkflowService.WorkflowRequest(
                    name, description, nodes, connections, settingsObj, staticData,
                    metaObj, pinData, Optional.ofNullable(projectId), tagNamesList);
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowResponse {
        private boolean success;
        private String message;
        private WorkflowDto workflow;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowsResponse {
        private boolean success;
        private String message;
        private List<WorkflowDto> workflows;
        private PageInfo pageInfo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowDto {
        private String id;
        private String name;
        private String description;
        private Boolean active;
        private Boolean isArchived;
        private List<Map<String, Object>> nodes;
        private Map<String, Object> connections;
        private WorkflowEntity.WorkflowSettings settings;
        private String versionId;
        private Integer versionCounter;
        private Integer triggerCount;
        private List<String> tagNames;
        private String projectId;
        private String ownerName;
        private String createdAt;
        private String updatedAt;

        public WorkflowDto(WorkflowEntity workflow) {
            this.id = workflow.getId();
            this.name = workflow.getName();
            this.description = workflow.getDescription();
            this.active = workflow.getActive();
            this.isArchived = workflow.getIsArchived();
            this.nodes = workflow.getNodes();
            this.connections = workflow.getConnections();
            this.settings = workflow.getSettings();
            this.versionId = workflow.getVersionId();
            this.versionCounter = workflow.getVersionCounter();
            this.triggerCount = workflow.getTriggerCount();
            this.tagNames = workflow.getTags() != null
                    ? workflow.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toList())
                    : List.of();
            this.projectId = workflow.getProject() != null ? workflow.getProject().getId().toString() : null;
            this.ownerName = workflow.getOwner() != null
                    ? workflow.getOwner().getFirstName() + " " + workflow.getOwner().getLastName()
                    : null;
            this.createdAt = workflow.getCreatedAt().toString();
            this.updatedAt = workflow.getUpdatedAt().toString();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PageInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;

        public PageInfo(Page<?> page) {
            this.currentPage = page.getNumber();
            this.pageSize = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.first = page.isFirst();
            this.last = page.isLast();
        }
    }
}