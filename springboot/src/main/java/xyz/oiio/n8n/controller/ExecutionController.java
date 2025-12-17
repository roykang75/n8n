package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.oiio.n8n.service.workflow.WorkflowRunnerService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rest/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final WorkflowRunnerService workflowRunnerService;

    @GetMapping("/{executionId}")
    public ResponseEntity<Map<String, Object>> getExecution(@PathVariable String executionId) {
        log.info("Fetching execution: {}", executionId);

        Map<String, Object> executionData = workflowRunnerService.getExecution(executionId);

        if (executionData == null) {
            log.warn("Execution not found: {}", executionId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(executionData);
    }
}
