package xyz.oiio.n8n.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.entity.User;
import xyz.oiio.n8n.service.user.UserService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rest/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @PatchMapping("/settings")
    public ResponseEntity<UserDto> updateSettings(@RequestBody Map<String, Object> updates) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getName();
        try {
            User user = userService.getUserById(userId);
            User.UserSettings settings = user.getSettings();
            if (settings == null) {
                settings = new User.UserSettings();
            }

            if (updates.containsKey("easyAIWorkflowOnboarded")) {
                settings.setEasyAIWorkflowOnboarded((Boolean) updates.get("easyAIWorkflowOnboarded"));
            }

            // Handle other potential settings updates here if needed in the future

            user.setSettings(settings); // Ensure settings are set back to user (though object reference might be
                                        // enough, this is safer)
            User updatedUser = userService.saveUser(user);

            return ResponseEntity.ok(new UserDto(updatedUser));
        } catch (UserService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating user settings", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class UserDto {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private xyz.oiio.n8n.entity.User.UserRole role;
        private Boolean isActive;
        private String createdAt;
        private String updatedAt;
        private xyz.oiio.n8n.entity.User.UserSettings settings;

        public UserDto(xyz.oiio.n8n.entity.User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.role = user.getRole();
            this.isActive = !user.getDisabled();
            this.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
            this.updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null;
            this.settings = user.getSettings();
        }
    }
}
