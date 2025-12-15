package xyz.oiio.n8n.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.oiio.n8n.entity.WebhookEntity;
import xyz.oiio.n8n.repository.WebhookRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;

    public WebhookEntity findWebhook(String method, String path) {
        // 1. Try static webhook
        Optional<WebhookEntity> staticWebhook = webhookRepository.findByWebhookPathAndMethod(path, method);
        if (staticWebhook.isPresent()) {
            return staticWebhook.get();
        }

        // 2. Try dynamic webhook
        return findDynamicWebhook(path, method);
    }

    private WebhookEntity findDynamicWebhook(String path, String method) {
        String[] parts = path.split("/");
        if (parts.length == 0)
            return null;

        String uuidSegment = parts[0];
        List<String> otherSegments = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));

        // Find potential candidates
        List<WebhookEntity> candidates = webhookRepository.findByWebhookIdAndMethodAndPathLength(uuidSegment, method,
                otherSegments.size());

        if (candidates.isEmpty()) {
            return null;
        }

        Set<String> requestSegments = Set.copyOf(otherSegments);

        WebhookEntity bestMatch = null;
        int maxMatches = -1;

        for (WebhookEntity candidate : candidates) {
            List<String> staticSegments = getStaticSegments(candidate.getWebhookPath());

            boolean allStaticSegmentsMatch = staticSegments.stream().allMatch(requestSegments::contains);

            if (allStaticSegmentsMatch && staticSegments.size() > maxMatches) {
                maxMatches = staticSegments.size();
                bestMatch = candidate;
            } else if (staticSegments.isEmpty() && bestMatch == null) {
                // edge case: if path is `:var`, match on anything (if check logic from Node.js)
                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    private List<String> getStaticSegments(String webhookPath) {
        if (webhookPath == null)
            return List.of();
        return Arrays.stream(webhookPath.split("/"))
                .filter(s -> !s.startsWith(":"))
                .collect(Collectors.toList());
    }
}
