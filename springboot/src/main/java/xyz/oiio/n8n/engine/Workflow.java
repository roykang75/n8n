package xyz.oiio.n8n.engine;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class Workflow {
    private String id;
    private String name;
    private List<Node> nodes;
    // Connections: SourceNode -> OutputIndex -> [ {node: DestNode, type: main,
    // index: InputIndex} ]
    private Map<String, Map<String, List<List<Map<String, Object>>>>> connections;

    // Helper to find start node
    public Node getNodeByName(String name) {
        return nodes.stream()
                .filter(n -> n.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
