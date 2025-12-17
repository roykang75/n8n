package xyz.oiio.n8n.push;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushWebSocketHandler extends TextWebSocketHandler {

    private final PushService pushService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String pushRef = extractPushRef(session);
        if (pushRef == null) {
            log.warn("WebSocket connection attempt without pushRef. Closing.");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        log.info("New WebSocket connection established: {}", pushRef);
        pushService.addWebSocketSession(pushRef, session);

        // Send initial confirmation if needed?
        // Node.js sends { type: 'open' } or similar?
        // Let's check Node.js logic later, but keeping connection open is key.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String pushRef = extractPushRef(session);
        if (pushRef != null) {
            log.info("WebSocket connection closed: {}", pushRef);
            pushService.removeWebSocketSession(pushRef);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received WebSocket message: {}", payload);

        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("type") && "heartbeat".equals(node.get("type").asText())) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception e) {
            // Ignore non-JSON messages or parsing errors
            log.trace("Ignored parsing error for message: {}", payload);
        }
    }

    private String extractPushRef(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        // Simple query parser
        String[] pairs = uri.getQuery().split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                if ("pushRef".equals(key)) {
                    return value;
                }
            }
        }
        return null;
    }
}
