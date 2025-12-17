package xyz.oiio.n8n.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.oiio.n8n.push.PushService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Component
public class WorkflowExecute {

    private final PushService pushService;
    private final ObjectMapper objectMapper;

    public WorkflowExecute(PushService pushService, ObjectMapper objectMapper) {
        this.pushService = pushService;
        this.objectMapper = objectMapper;
    }

    public void run(Workflow workflow, String executionId, String pushRef) {
        log.info("WorkflowExecute: Starting execution {}", executionId);

        try {
            // 1. Send ExecutionStarted
            sendExecutionStarted(executionId, pushRef);

            // 2. Find Start Node (Assume first for now or find trigger)
            Node startNode = workflow.getNodes().stream()
                    .filter(n -> n.getType().contains("chatTrigger") || n.getType().contains("Trigger"))
                    .findFirst()
                    .orElse(workflow.getNodes().get(0));

            log.info("Start node: {}", startNode.getName());

            // Queue for BFS traversal
            Queue<Node> queue = new LinkedList<>();
            queue.add(startNode);

            // Track processed
            Set<String> processed = new HashSet<>();

            // Pass data between nodes
            Map<String, Object> runData = new HashMap<>(); // nodeName -> outputData

            while (!queue.isEmpty()) {
                Node currentNode = queue.poll();
                if (processed.contains(currentNode.getName()))
                    continue;
                processed.add(currentNode.getName());

                log.info("Executing node: {}", currentNode.getName());

                // 3. Emit NodeExecuteBefore
                sendNodeExecuteBefore(executionId, pushRef, currentNode.getName());

                // 4. EXECUTE NODE (Simulated or Bridged)
                // In a real engine, we call the JS worker here with input data.
                // For now, we simulate the text generation for AI Agent.
                String outputText = "Processed " + currentNode.getName();
                if (currentNode.getType().contains("Agent") || currentNode.getType().contains("Chain")) {
                    outputText = "Hello from Java Engine! I traced the graph correctly.";
                }

                // Simulate processing time
                Thread.sleep(500);

                // 5. Emit NodeExecuteAfter
                sendNodeExecuteAfter(executionId, pushRef, currentNode.getName());

                // 6. Emit NodeExecuteAfterData (The important output)
                sendNodeExecuteAfterData(executionId, pushRef, currentNode.getName(), outputText);

                // 7. Find next nodes
                if (workflow.getConnections() != null && workflow.getConnections().containsKey(currentNode.getName())) {
                    Map<String, List<List<Map<String, Object>>>> outputs = workflow.getConnections()
                            .get(currentNode.getName());
                    if (outputs.containsKey("main")) {
                        for (List<Map<String, Object>> connectionList : outputs.get("main")) {
                            for (Map<String, Object> conn : connectionList) {
                                String nextNodeName = (String) conn.get("node");
                                Node nextNode = workflow.getNodeByName(nextNodeName);
                                if (nextNode != null) {
                                    queue.add(nextNode);
                                }
                            }
                        }
                    }
                }
            }

            // 8. ExecutionFinished
            sendExecutionFinished(executionId, pushRef);

        } catch (Exception e) {
            log.error("Execution failed", e);
        }
    }

    // --- Helper Methods to Emit Events (Copied/Refined from Service) ---

    private void sendExecutionStarted(String executionId, String pushRef) {
        Map<String, Object> data = new HashMap<>();
        data.put("executionId", executionId);
        data.put("mode", "manual");
        data.put("startedAt", new Date());
        pushService.send("executionStarted", data, pushRef);
    }

    private void sendNodeExecuteBefore(String executionId, String pushRef, String nodeName) {
        Map<String, Object> data = new HashMap<>();
        data.put("executionId", executionId);
        data.put("nodeName", nodeName);
        pushService.send("nodeExecuteBefore", data, pushRef);
    }

    private void sendNodeExecuteAfter(String executionId, String pushRef, String nodeName) {
        Map<String, Object> data = new HashMap<>();
        data.put("executionId", executionId);
        data.put("nodeName", nodeName);
        pushService.send("nodeExecuteAfter", data, pushRef);
    }

    private void sendNodeExecuteAfterData(String executionId, String pushRef, String nodeName, String outputText) {
        Map<String, Object> data = new HashMap<>();
        data.put("executionId", executionId);
        data.put("nodeName", nodeName);

        // Correct Structure
        Map<String, Object> json = new HashMap<>();
        json.put("output", outputText);
        json.put("text", outputText);

        List<Map<String, Object>> items = List.of(Map.of("json", json));
        List<List<Map<String, Object>>> main = List.of(items);

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("startTime", System.currentTimeMillis());
        taskData.put("executionTime", 100);
        taskData.put("executionStatus", "success");
        taskData.put("data", Map.of("main", main));

        data.put("data", taskData);
        data.put("itemCountByConnectionType", Map.of("main", List.of(1)));

        pushService.send("nodeExecuteAfterData", data, pushRef);
    }

    private void sendExecutionFinished(String executionId, String pushRef) {
        Map<String, Object> data = new HashMap<>();
        data.put("executionId", executionId);
        data.put("status", "success");
        pushService.send("executionFinished", data, pushRef);
    }
}
