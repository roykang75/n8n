package xyz.oiio.n8n.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.service.user.UserService;
import xyz.oiio.n8n.service.user.UserService.UserRequest;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            xyz.oiio.n8n.entity.User createdUser = userService.createUser(request.toServiceRequest());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UserResponse.success("User created successfully", createdUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(UserResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        try {
            xyz.oiio.n8n.entity.User user = userService.getUserById(userId);
            return ResponseEntity.ok(UserResponse.success("User retrieved successfully", user));
        } catch (UserService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String userId = authentication.getName();
            try {
                xyz.oiio.n8n.entity.User user = userService.getUserById(userId);
                return ResponseEntity.ok(UserResponse.success("Current user retrieved successfully", user));
            } catch (UserService.NotFoundException e) {
                return ResponseEntity.notFound().build();
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<xyz.oiio.n8n.entity.User> users = userService.getAllActiveUsers();
        List<UserResponse> response = users.stream()
                .map(user -> UserResponse.success("User retrieved successfully", user))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String userId,
                                                  @Valid @RequestBody UpdateUserRequest request) {
        try {
            xyz.oiio.n8n.entity.User user = userService.getUserById(userId);

            // 업데이트 가능한 필드 적용
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }
            if (request.getDisabled() != null) {
                user.setDisabled(request.getDisabled());
            }

            xyz.oiio.n8n.entity.User updatedUser = userService.saveUser(user);
            return ResponseEntity.ok(UserResponse.success("User updated successfully", updatedUser));
        } catch (UserService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (UserService.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DTOs
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateUserRequest {
        private String email;
        private String firstName;
        private String lastName;
        private String password;
        private xyz.oiio.n8n.entity.User.UserRole role;

        public UserRequest toServiceRequest() {
            return new UserRequest(
                email,
                firstName,
                lastName,
                password,
                role != null ? role : xyz.oiio.n8n.entity.User.UserRole.USER,
                null // settings
            );
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateUserRequest {
        private String firstName;
        private String lastName;
        private Boolean disabled;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserResponse {
        private boolean success;
        private String message;
        private UserDto user;

        public static UserResponse success(String message, xyz.oiio.n8n.entity.User user) {
            return new UserResponse(true, message, new UserDto(user));
        }

        public static UserResponse error(String message) {
            return new UserResponse(false, message, null);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDto {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private xyz.oiio.n8n.entity.User.UserRole role;
        private Boolean isActive;
        private String createdAt;
        private String updatedAt;

        public UserDto(xyz.oiio.n8n.entity.User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.role = user.getRole();
            this.isActive = !user.getDisabled();
            this.createdAt = user.getCreatedAt().toString();
            this.updatedAt = user.getUpdatedAt().toString();
        }
    }
}