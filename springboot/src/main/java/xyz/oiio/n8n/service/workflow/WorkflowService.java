package xyz.oiio.n8n.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.oiio.n8n.entity.Project;
import xyz.oiio.n8n.entity.TagEntity;
import xyz.oiio.n8n.entity.User;
import xyz.oiio.n8n.entity.WorkflowEntity;
import xyz.oiio.n8n.repository.ProjectRepository;
import xyz.oiio.n8n.repository.TagRepository;
import xyz.oiio.n8n.repository.WorkflowRepository;
import xyz.oiio.n8n.util.NanoIdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;

    @Transactional
    public WorkflowEntity createWorkflow(WorkflowRequest request, User owner) {
        // Check if workflow name already exists for user
        if (workflowRepository.existsByNameAndOwner(request.getName(), owner)) {
            throw new IllegalArgumentException("Workflow name already exists: " + request.getName());
        }

        WorkflowEntity workflow = WorkflowEntity.builder()
                .id(NanoIdGenerator.randomNanoId())
                .name(request.getName())
                .description(request.getDescription())
                .active(false)
                .isArchived(false)
                .nodes(request.getNodes())
                .connections(request.getConnections())
                .settings(request.getSettings())
                .staticData(request.getStaticData())
                .meta(request.getMeta())
                .versionId(UUID.randomUUID().toString())
                .versionCounter(0)
                .triggerCount(0)
                .pinData(request.getPinData())
                .owner(owner)
                .projectId(request.getProjectId().filter(id -> id.matches("\\d+"))
                    .map(Long::parseLong)
                    .orElse(null))
                .tags(getTagsFromNames(request.getTagNames()))
                .build();

        WorkflowEntity savedWorkflow = workflowRepository.save(workflow);
        log.info("Created new workflow with id: {}", savedWorkflow.getId());
        return savedWorkflow;
    }

    @Transactional
    public WorkflowEntity updateWorkflow(String workflowId, WorkflowRequest request) {
        WorkflowEntity workflow = getWorkflowById(workflowId);

        if (!workflow.getName().equals(request.getName()) &&
            workflowRepository.existsByNameAndOwner(request.getName(), workflow.getOwner())) {
            throw new IllegalArgumentException("Workflow name already exists: " + request.getName());
        }

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setNodes(request.getNodes());
        workflow.setConnections(request.getConnections());
        workflow.setSettings(request.getSettings());
        workflow.setStaticData(request.getStaticData());
        workflow.setMeta(request.getMeta());
        workflow.setPinData(request.getPinData());
        workflow.setVersionCounter(workflow.getVersionCounter() + 1);
        workflow.setVersionId(UUID.randomUUID().toString());

        workflow.setProjectId(request.getProjectId().filter(id -> id.matches("\\d+"))
            .map(Long::parseLong)
            .orElse(workflow.getProjectId()));

        // Update tags
        workflow.setTags(getTagsFromNames(request.getTagNames()));

        WorkflowEntity savedWorkflow = workflowRepository.save(workflow);
        log.info("Updated workflow with id: {}", savedWorkflow.getId());
        return savedWorkflow;
    }

    @Transactional
    public void deleteWorkflow(String workflowId) {
        WorkflowEntity workflow = getWorkflowById(workflowId);
        workflow.setIsArchived(true);
        workflowRepository.save(workflow);
        log.info("Archived workflow with id: {}", workflowId);
    }

    @Transactional
    public WorkflowEntity activateWorkflow(String workflowId) {
        WorkflowEntity workflow = getWorkflowById(workflowId);
        workflow.setActive(true);
        workflowRepository.save(workflow);
        log.info("Activated workflow with id: {}", workflowId);
        return workflow;
    }

    @Transactional
    public WorkflowEntity deactivateWorkflow(String workflowId) {
        WorkflowEntity workflow = getWorkflowById(workflowId);
        workflow.setActive(false);
        workflowRepository.save(workflow);
        log.info("Deactivated workflow with id: {}", workflowId);
        return workflow;
    }

    @Transactional(readOnly = true)
    public WorkflowEntity getWorkflowById(String workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NotFoundException("Workflow not found with id: " + workflowId));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowEntity> getWorkflowsByUser(User user, Pageable pageable) {
        return workflowRepository.findByOwnerAndIsArchivedFalse(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowEntity> getAccessibleWorkflows(String userId, Pageable pageable) {
        return workflowRepository.findAccessibleWorkflows(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<WorkflowEntity> getWorkflowsByUser(User user) {
        return workflowRepository.findByOwnerAndIsArchivedFalse(user);
    }

    @Transactional(readOnly = true)
    public List<WorkflowEntity> getWorkflowsByProject(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return workflowRepository.findByProjectIdAndIsArchivedFalse(projectId);
    }

    @Transactional(readOnly = true)
    public List<WorkflowEntity> getWorkflowsByTagName(String tagName) {
        return workflowRepository.findByTagName(tagName);
    }

    @Transactional
    public WorkflowEntity incrementTriggerCount(String workflowId) {
        WorkflowEntity workflow = getWorkflowById(workflowId);
        workflow.setTriggerCount(workflow.getTriggerCount() + 1);
        return workflowRepository.save(workflow);
    }

    private List<TagEntity> getTagsFromNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        return tagNames.stream()
                .map(tagName -> tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            TagEntity newTag = new TagEntity();
                            newTag.setName(tagName);
                            return tagRepository.save(newTag);
                        }))
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowRequest {
        private String name;
        private String description;
        private List<Object> nodes;
        private Object connections;
        private WorkflowEntity.WorkflowSettings settings;
        private Object staticData;
        private WorkflowEntity.WorkflowMeta meta;
        private Object pinData;
        private Optional<String> projectId = Optional.empty();
        private List<String> tagNames = List.of();
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}