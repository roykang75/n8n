package xyz.oiio.n8n.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.entity.WebhookEntity;
import xyz.oiio.n8n.service.WebhookService;

@RestController
@RequestMapping("/rest/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/find")
    public ResponseEntity<WebhookEntity> findWebhook(@RequestBody FindWebhookRequest request) {
        WebhookEntity webhook = webhookService.findWebhook(request.getMethod(), request.getPath());
        if (webhook == null) {
            // Node.js returns null, which translates to 200 OK with empty body/null in
            // Express/NestJS usually.
            // Returning 404 causes "Error" in frontend.
            // We return 200 OK with null body to match Node.js behavior.
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(webhook);
    }

    @Data
    public static class FindWebhookRequest {
        private String path;
        private String method;
    }
}
