package xyz.oiio.n8n.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final ObjectMapper objectMapper;
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String pushRef) {
        // Timeout 0 means infinite (or rely on server config)
        SseEmitter emitter = new SseEmitter(0L);

        sseEmitters.put(pushRef, emitter);

        emitter.onCompletion(() -> {
            log.debug("Emitter completed: {}", pushRef);
            sseEmitters.remove(pushRef);
        });
        emitter.onTimeout(() -> {
            log.debug("Emitter timeout: {}", pushRef);
            emitter.complete();
            sseEmitters.remove(pushRef);
        });
        emitter.onError((e) -> {
            log.debug("Emitter error: {}", pushRef, e);
            emitter.complete();
            sseEmitters.remove(pushRef);
        });

        // Send confirmation
        try {
            // SseEmitter can't easily send comments, so sending empty data or similar
            emitter.send(SseEmitter.event().name("open").data(""));
        } catch (IOException e) {
            log.error("Failed to send initial open event", e);
            sseEmitters.remove(pushRef);
        }

        return emitter;
    }

    public void addWebSocketSession(String pushRef, WebSocketSession session) {
        wsSessions.put(pushRef, session);
    }

    public void removeWebSocketSession(String pushRef) {
        wsSessions.remove(pushRef);
    }

    public void sendToAll(Object data) {
        // Send to SSE
        sseEmitters.forEach((pushRef, emitter) -> {
            try {
                emitter.send(data);
            } catch (IOException e) {
                sseEmitters.remove(pushRef);
            }
        });

        // Send to WebSocket
        String json = "";
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize push data", e);
            return;
        }

        final String payload = json;
        wsSessions.forEach((pushRef, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.error("WebSocket send error", e);
                }
            } else {
                wsSessions.remove(pushRef);
            }
        });
    }

    // Heartbeat mechanism (can be scheduled)
    public void sendHeartbeat() {
        sseEmitters.forEach((pushRef, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                sseEmitters.remove(pushRef);
            }
        });
    }

    public void send(String type, Object data, String pushRef) {
        if (pushRef == null) {
            return;
        }

        Map<String, Object> message = Map.of("type", type, "data", data);

        // Send to WebSocket if connected
        WebSocketSession session = wsSessions.get(pushRef);
        if (session != null && session.isOpen()) {
            try {
                String payload = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to {}", pushRef, e);
            }
        }

        // Send to SSE if connected
        SseEmitter emitter = sseEmitters.get(pushRef);
        if (emitter != null) {
            try {
                emitter.send(message);
            } catch (IOException e) {
                log.debug("Failed to send SSE message to {}", pushRef, e);
                sseEmitters.remove(pushRef);
            }
        }
    }
}
