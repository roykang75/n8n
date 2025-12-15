package xyz.oiio.n8n.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PushService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String pushRef) {
        // Timeout 0 means infinite (or rely on server config)
        // Original n8n sets socket timeout to 0
        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(pushRef, emitter);

        emitter.onCompletion(() -> {
            log.debug("Emitter completed: {}", pushRef);
            emitters.remove(pushRef);
        });
        emitter.onTimeout(() -> {
            log.debug("Emitter timeout: {}", pushRef);
            emitter.complete();
            emitters.remove(pushRef);
        });
        emitter.onError((e) -> {
            log.debug("Emitter error: {}", pushRef, e);
            emitter.complete();
            emitters.remove(pushRef);
        });

        // Send initial :ok message as per n8n protocol
        try {
            // :ok is a comment in SSE, or just data? n8n uses ':ok\n\n' which looks like a
            // comment or custom keep-alive
            // checking sse.push.ts: res.write(':ok\n\n'); -> This is likely interpreted as
            // a comment by standard EventSource,
            // or a custom heartbeat if they read raw stream.
            // Spring SseEmitter sends "data:" prefix for .send().
            // To send raw comments/custom strings we might need to bypass or use specific
            // method if available,
            // but SseEmitter is structured.
            // However, n8n client might expect exactly ":ok".
            // Spring SseEmitter doesn't easily support raw writes without "data:",
            // "event:", etc.
            // But let's try standard SseEmitter.event() builder.

            // Actually, SseEmitter.send(object) sends "data: object\n\n".
            // If we need ":ok", that's a comment.
            // SseEmitter can send comments? No direct API for comments in basic usage.
            // but we can try to send it as a heartbeat if we assume standard usage.
            // Only way to send raw ":ok" with SseEmitter is tricky.
            // Let's assume sending a "data: ok" or similar might be enough, OR
            // the custom n8n client parses it.
            // Wait, standard EventSource ignores lines starting with ':'.
            // So ':ok' is a comment. It signals "connected" effectively.

            // For now, let's just trigger a dummy event or comment if possible.
            // If SseEmitter issues are found, we might need a raw Controller writing to
            // OutputStream.
            // But let's try sending a heartbeat event with empty data to establish
            // connection.
            emitter.send(SseEmitter.event().comment("ok"));

        } catch (IOException e) {
            log.error("Failed to send initial ok", e);
            emitters.remove(pushRef);
        }

        return emitter;
    }

    // Heartbeat mechanism (can be scheduled)
    public void sendHeartbeat() {
        emitters.forEach((pushRef, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                // Dead emitter, remove it
                emitters.remove(pushRef);
            }
        });
    }

    public void sendToAll(Object data) {
        emitters.forEach((pushRef, emitter) -> {
            try {
                emitter.send(data);
            } catch (IOException e) {
                emitters.remove(pushRef);
            }
        });
    }
}
