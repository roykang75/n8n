package xyz.oiio.n8n.engine;

import lombok.Data;
import java.util.Map;

@Data
public class Node {
    private String id;
    private String name;
    private String type;
    private int typeVersion;
    private int[] position;
    private String authentication; // credential ID
    private Map<String, Object> parameters;
    private Map<String, Object> credentials;
    // Add other fields as needed
}
