package xyz.oiio.n8n.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import xyz.oiio.n8n.entity.CredentialsEntity;
import xyz.oiio.n8n.repository.CredentialsRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/rest/dynamic-node-parameters")
@RequiredArgsConstructor
public class DynamicNodeParametersController {

    private final CredentialsRepository credentialsRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @PostMapping("/options")
    public ResponseEntity<Map<String, Object>> getOptions(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /dynamic-node-parameters/options - request: {}", request);

        try {
            // Extract loadOptions from request
            @SuppressWarnings("unchecked")
            Map<String, Object> loadOptions = (Map<String, Object>) request.get("loadOptions");

            if (loadOptions == null) {
                log.warn("No loadOptions in request");
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }

            // Extract routing configuration
            @SuppressWarnings("unchecked")
            Map<String, Object> routing = (Map<String, Object>) loadOptions.get("routing");
            if (routing == null) {
                log.warn("No routing in loadOptions");
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }

            // Get credential baseUrl
            String baseUrl = getCredentialBaseUrl(request);
            if (baseUrl == null) {
                log.warn("Could not resolve credential baseUrl");
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }

            // Execute the request defined in routing
            @SuppressWarnings("unchecked")
            Map<String, Object> routingRequest = (Map<String, Object>) routing.get("request");
            String method = (String) routingRequest.getOrDefault("method", "GET");
            String url = (String) routingRequest.get("url");

            // Call external API
            String fullUrl = baseUrl.replaceAll("/$", "") + url;
            log.info("Calling external API: {} {}", method, fullUrl);

            ResponseEntity<String> response = callExternalApi(method, fullUrl);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("External API call failed: {}", response.getStatusCode());
                return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
            }

            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.getBody());

            // Apply postReceive transformations
            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) routing.get("output");
            List<Map<String, Object>> result = applyPostReceive(responseJson, output);

            log.info("Returning {} options", result.size());
            return ResponseEntity.ok(Map.of("data", result));

        } catch (Exception e) {
            log.error("Error in getOptions: ", e);
            return ResponseEntity.ok(Map.of("data", Collections.emptyList()));
        }
    }

    private String getCredentialBaseUrl(Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> credentials = (Map<String, Object>) request.get("credentials");
            if (credentials == null || credentials.isEmpty()) {
                return null;
            }

            // Get first credential entry
            Map.Entry<String, Object> credentialEntry = credentials.entrySet().iterator().next();
            @SuppressWarnings("unchecked")
            Map<String, Object> credentialInfo = (Map<String, Object>) credentialEntry.getValue();
            String credentialId = (String) credentialInfo.get("id");

            if (credentialId == null) {
                return null;
            }

            // Lookup credential from database
            Optional<CredentialsEntity> credOpt = credentialsRepository.findById(credentialId);
            if (credOpt.isEmpty()) {
                log.warn("Credential not found: {}", credentialId);
                return null;
            }

            CredentialsEntity credential = credOpt.get();
            String data = credential.getData();

            // Parse credential data to get baseUrl
            // Data format might be: {"baseUrl":"http://localhost:11434"}
            // or it might be encrypted, for now try to parse as JSON
            if (data != null && data.startsWith("{")) {
                JsonNode dataNode = objectMapper.readTree(data);
                if (dataNode.has("baseUrl")) {
                    return dataNode.get("baseUrl").asText();
                }
            }

            // If data is not JSON, it might be the baseUrl directly or encrypted
            // For Ollama, default to localhost:11434
            log.warn("Could not parse credential data, using default Ollama URL");
            return "http://localhost:11434";

        } catch (Exception e) {
            log.error("Error getting credential baseUrl: ", e);
            return "http://localhost:11434"; // Default fallback
        }
    }

    private ResponseEntity<String> callExternalApi(String method, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            if ("GET".equalsIgnoreCase(method)) {
                return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } else if ("POST".equalsIgnoreCase(method)) {
                return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            } else {
                return restTemplate.exchange(url, HttpMethod.valueOf(method), entity, String.class);
            }
        } catch (Exception e) {
            log.error("Error calling external API: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> applyPostReceive(JsonNode responseJson, Map<String, Object> output) {
        if (output == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> postReceive = (List<Map<String, Object>>) output.get("postReceive");
        if (postReceive == null || postReceive.isEmpty()) {
            return Collections.emptyList();
        }

        List<JsonNode> items = new ArrayList<>();
        items.add(responseJson);

        for (Map<String, Object> transformation : postReceive) {
            String type = (String) transformation.get("type");
            Map<String, Object> properties = (Map<String, Object>) transformation.get("properties");

            switch (type) {
                case "rootProperty":
                    items = applyRootProperty(items, properties);
                    break;
                case "setKeyValue":
                    return applySetKeyValue(items, properties);
                case "sort":
                    // Sort will be applied after setKeyValue
                    break;
            }
        }

        return Collections.emptyList();
    }

    private List<JsonNode> applyRootProperty(List<JsonNode> items, Map<String, Object> properties) {
        String property = (String) properties.get("property");
        List<JsonNode> result = new ArrayList<>();

        for (JsonNode item : items) {
            if (item.has(property)) {
                JsonNode prop = item.get(property);
                if (prop.isArray()) {
                    prop.forEach(result::add);
                } else {
                    result.add(prop);
                }
            }
        }

        return result;
    }

    private List<Map<String, Object>> applySetKeyValue(List<JsonNode> items, Map<String, Object> properties) {
        String nameTemplate = (String) properties.get("name");
        String valueTemplate = (String) properties.get("value");

        List<Map<String, Object>> result = new ArrayList<>();

        for (JsonNode item : items) {
            String name = resolveTemplate(nameTemplate, item);
            String value = resolveTemplate(valueTemplate, item);

            Map<String, Object> option = new LinkedHashMap<>();
            option.put("name", name);
            option.put("value", value);
            result.add(option);
        }

        // Sort by name
        result.sort(Comparator.comparing(o -> (String) o.get("name")));

        return result;
    }

    private String resolveTemplate(String template, JsonNode item) {
        if (template == null) {
            return "";
        }

        // Handle expression like: ={{$responseItem.name}}
        if (template.startsWith("={{") && template.endsWith("}}")) {
            String expression = template.substring(3, template.length() - 2);
            // Parse $responseItem.fieldName
            if (expression.startsWith("$responseItem.")) {
                String fieldName = expression.substring("$responseItem.".length());
                if (item.has(fieldName)) {
                    return item.get(fieldName).asText();
                }
            }
        }

        return template;
    }
}
