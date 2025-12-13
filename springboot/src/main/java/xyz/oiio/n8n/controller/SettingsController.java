package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.oiio.n8n.entity.User;
import xyz.oiio.n8n.repository.UserRepository;

import java.util.HashMap;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rest")
@RequiredArgsConstructor
public class SettingsController {

        private final UserRepository userRepository;

        @GetMapping("/settings")
        public Map<String, Object> getSettings() {
                Map<String, Object> settings = new HashMap<>();

                boolean ownerExists = !userRepository.findByRole(User.UserRole.OWNER).isEmpty();

                // User Management Settings
                Map<String, Object> userManagement = new HashMap<>();
                userManagement.put("quota", -1);
                userManagement.put("authenticationMethod", "email");
                userManagement.put("smtpSetup", false);
                userManagement.put("showSetupOnFirstLoad", !ownerExists);
                settings.put("userManagement", userManagement);

                // SSO Settings (Disabled by default)
                Map<String, Object> sso = new HashMap<>();
                sso.put("saml", Map.of("loginEnabled", false));
                sso.put("ldap", Map.of("loginEnabled", false));
                sso.put("oidc", Map.of("loginEnabled", false));
                settings.put("sso", sso);

                // Enterprise Settings
                Map<String, Object> enterprise = new HashMap<>();
                enterprise.put("sharing", false);
                enterprise.put("ldap", false);
                enterprise.put("saml", false);
                enterprise.put("oidc", false);
                enterprise.put("mfaEnforcement", false);
                enterprise.put("logStreaming", false);
                enterprise.put("advancedExecutionFilters", false);
                enterprise.put("variables", false);
                enterprise.put("sourceControl", false);
                enterprise.put("auditLogs", false);
                enterprise.put("externalSecrets", false);
                // Projects settings - required by frontend projects.store.ts
                enterprise.put("projects", Map.of(
                                "team", Map.of("limit", -1)));
                settings.put("enterprise", enterprise);

                // General Settings
                settings.put("pushBackend", "websocket"); // Use WebSocket
                settings.put("endpointWebhook", "http://localhost:5678/webhook");
                settings.put("urlBaseWebhook", "http://localhost:5678");
                settings.put("urlBaseEditor", "http://localhost:8080");
                settings.put("urlBaseApi", "http://localhost:5678/rest");
                settings.put("concurrency", 10);
                settings.put("executionTimeout", 3600);
                settings.put("releaseChannel", "stable");
                settings.put("versionCli", "1.0.0");
                settings.put("nodeJsVersion", "21");
                settings.put("defaultLocale", "en");
                settings.put("license", Map.of("planName", "Community"));

                // PostHog / Telemetry
                settings.put("telemetry", Map.of("enabled", false));
                settings.put("posthog", Map.of("enabled", false));

                // Version Notifications
                settings.put("versionNotifications", Map.of("enabled", false, "endpoint", "", "whatsNewEnabled", false,
                                "whatsNewEndpoint", "", "infoUrl", ""));

                // Feature flags / Other
                settings.put("aiAssistant", Map.of("enabled", false, "setup", false));
                settings.put("askAi", Map.of("enabled", false));
                settings.put("aiBuilder", Map.of("enabled", false, "setup", false));
                settings.put("aiCredits", Map.of("enabled", false, "credits", 0));

                settings.put("publicApi",
                                Map.of("enabled", true, "latestVersion", 1, "path", "/rest/public", "swaggerUi",
                                                Map.of("enabled", true)));

                settings.put("endpoints", Map.of(
                                "rest", "/rest"));

                // Essential booleans
                settings.put("pruning", Map.of("isEnabled", true));
                settings.put("security", Map.of("blockFileAccessToN8nFiles", true));
                settings.put("mfa", Map.of("enabled", false, "enforced", false));
                settings.put("templates", Map.of("enabled", true, "host", ""));
                settings.put("authCookie", Map.of("secure", false));

                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("data", settings);

                return wrapper;
        }
}
