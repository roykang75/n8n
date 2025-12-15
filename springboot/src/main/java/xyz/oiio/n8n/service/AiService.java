package xyz.oiio.n8n.service;

import org.springframework.stereotype.Service;
import xyz.oiio.n8n.dto.AiChatRequest;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

@Service
public class AiService {

    public Iterator<Object> chat(AiChatRequest payload, Object user) {
        // Mock response
        // Structure matches Node.js chunk: { messages: [ { role: 'assistant', type:
        // 'text', content: '...' } ] }
        Map<String, Object> message = Map.of(
                "role", "assistant",
                "type", "text",
                "content", "AI Assistant is not currently supported in this backend environment.");
        Map<String, Object> chunk = Map.of("messages", Collections.singletonList(message));

        return Collections.<Object>singletonList(chunk).iterator();
    }

    public Iterator<Object> build(Map<String, Object> payload, Object user) {
        // Mock response for build
        Map<String, Object> message = Map.of(
                "role", "assistant",
                "type", "text",
                "content", "AI Workflow Building is not currently supported in this backend environment.");
        Map<String, Object> chunk = Map.of("messages", Collections.singletonList(message));

        return Collections.<Object>singletonList(chunk).iterator();
    }

    public Object applySuggestion(Map<String, Object> payload, Object user) {
        return Collections.emptyMap();
    }

    public Object askAi(Map<String, Object> payload, Object user) {
        return Map.of("text", "AI is not supported.");
    }

    public Object freeCredits(Map<String, Object> payload, Object user) {
        return Collections.emptyMap();
    }

    public Object getBuilderCredits(Object user) {
        return Map.of("credits", 0);
    }
}
