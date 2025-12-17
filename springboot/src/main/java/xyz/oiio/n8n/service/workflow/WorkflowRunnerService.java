package xyz.oiio.n8n.service.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.oiio.n8n.controller.WorkflowController;
import xyz.oiio.n8n.push.PushService;
import xyz.oiio.n8n.engine.WorkflowExecute;
import xyz.oiio.n8n.engine.Workflow;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WorkflowRunnerService {

    private final PushService pushService;
    private final WorkflowExecute workflowExecute;
    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, Object>> executions = new ConcurrentHashMap<>();

    // Manual constructor since we are using fields
    public WorkflowRunnerService(PushService pushService, WorkflowExecute workflowExecute, ObjectMapper objectMapper) {
        this.pushService = pushService;
        this.workflowExecute = workflowExecute;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getExecution(String executionId) {
        return executions.get(executionId);
    }

    @Async
    public void runWorkflow(String executionId, String pushRef, WorkflowController.RunWorkflowRequest request) {
        log.info("Starting native workflow execution for id: {}, pushRef: {}", executionId, pushRef);

        // Store initial execution state
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("id", executionId);
        executionData.put("mode", "manual");
        executionData.put("status", "running");
        executionData.put("startedAt", new Date());
        executionData.put("finished", false);
        executionData.put("data", Map.of("resultData", Map.of("runData", new HashMap<>())));

        executions.put(executionId, executionData);

        if (pushRef == null || pushRef.isEmpty()) {
            log.warn("No pushRef provided. execution: {}", executionId);
        }

        try {
            // Parse Workflow from request
            Map<String, Object> workflowData = request.getWorkflowData();
            if (workflowData == null) {
                throw new IllegalArgumentException("Workflow data missing");
            }

            Workflow workflow = objectMapper.convertValue(workflowData, Workflow.class);

            // Execute using Native Engine
            workflowExecute.run(workflow, executionId, pushRef);

            executionData.put("status", "success");
            executionData.put("finished", true);
            executionData.put("stoppedAt", new Date());

        } catch (Exception e) {
            log.error("Error during native execution", e);
            executionData.put("status", "error");
            executionData.put("finished", true);
            executionData.put("stoppedAt", new Date());
        }
    }

}
