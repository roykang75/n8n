package xyz.oiio.n8n.dto;

import lombok.Data;
import java.util.Map;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatRequest {
    private Map<String, Object> payload;
    private String sessionId;
}
