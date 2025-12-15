package xyz.oiio.n8n.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import xyz.oiio.n8n.dto.AiChatRequest;
import xyz.oiio.n8n.service.AiService;

import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({ "/rest/ai", "/ai" })
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    // STREAM_SEPARATOR from Node.js (packages/cli/src/constants.ts)
    // using unicode escapes for safety: ⧉⇋⇋➽⌑⧉§§\n
    private static final String STREAM_SEPARATOR = "\u29C9\u21CB\u21CB\u27BD\u2311\u29C9\u00A7\u00A7\n";

    @PostMapping("/chat")
    public ResponseEntity<StreamingResponseBody> chat(@RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Received AI Chat Request: {}", request);
        Iterator<Object> responseStream = aiService.chat(request, user);

        StreamingResponseBody stream = outputStream -> {
            while (responseStream.hasNext()) {
                Object chunk = responseStream.next();
                String json = objectMapper.writeValueAsString(chunk);
                outputStream.write((json + STREAM_SEPARATOR).getBytes());
                outputStream.flush();
            }
        };

        return ResponseEntity.ok()
                // Node.js uses 'application/json-lines' custom type or similar.
                .contentType(MediaType.parseMediaType("application/json-lines"))
                .body(stream);
    }

    @PostMapping("/build")
    public ResponseEntity<StreamingResponseBody> build(@RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Received AI Build Request");
        // AI Builder Mock (Streaming)
        Iterator<Object> responseStream = aiService.build(payload, user);

        StreamingResponseBody stream = outputStream -> {
            while (responseStream.hasNext()) {
                Object chunk = responseStream.next();
                String json = objectMapper.writeValueAsString(chunk);
                outputStream.write((json + STREAM_SEPARATOR).getBytes());
                outputStream.flush();
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/json-lines"))
                .body(stream);
    }

    @PostMapping("/chat/apply-suggestion")
    public ResponseEntity<Object> applySuggestion(@RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(aiService.applySuggestion(payload, user));
    }

    @PostMapping("/ask-ai")
    public ResponseEntity<Object> askAi(@RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(aiService.askAi(payload, user));
    }

    @PostMapping("/free-credits")
    public ResponseEntity<Object> freeCredits(@RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(aiService.freeCredits(payload, user));
    }

    @org.springframework.web.bind.annotation.GetMapping("/build/credits")
    public ResponseEntity<Object> getBuilderCredits(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(aiService.getBuilderCredits(user));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Object> getSessions(@RequestBody Map<String, Object> payload) {
        // Mock session list (empty)
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }

    @PostMapping("/sessions/metadata")
    public ResponseEntity<Object> getSessionsMetadata(@RequestBody Map<String, Object> payload) {
        // Mock metadata
        return ResponseEntity.ok(java.util.Collections.emptyMap());
    }
}
