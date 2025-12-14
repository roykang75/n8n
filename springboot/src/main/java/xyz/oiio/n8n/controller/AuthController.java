package xyz.oiio.n8n.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import xyz.oiio.n8n.security.JwtTokenProvider;
import xyz.oiio.n8n.service.user.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/rest")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        log.info("=== Login request received ===");
        log.info("Email: {}", loginRequest.getEmail());
        try {
            log.info("Attempting authentication for: {}", loginRequest.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));
            log.info("Authentication successful for: {}", loginRequest.getEmail());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            log.info("JWT generated successfully");

            // Set cookie
            addAuthCookie(response, jwt);

            // Get user details
            Optional<xyz.oiio.n8n.entity.User> userOpt = userService.findByEmail(loginRequest.getEmail());
            Map<String, Object> userInfo = new HashMap<>();
            userOpt.ifPresent(user -> {
                userInfo.put("id", user.getId());
                userInfo.put("email", user.getEmail());
                userInfo.put("firstName", user.getFirstName());
                userInfo.put("lastName", user.getLastName());
                userInfo.put("roles", List.of(user.getRole().name().toLowerCase()));
            });

            return ResponseEntity.ok(Map.of("data", userInfo));
        } catch (Exception e) {
            log.error("Authentication failed for user: {} - Exception type: {} - Message: {}",
                    loginRequest.getEmail(), e.getClass().getName(), e.getMessage());
            log.error("Full stack trace:", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password", "details", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
            HttpServletResponse response) {
        try {
            if (userService.findByEmail(registerRequest.getEmail()).isPresent()) {
                throw new RuntimeException("Email is already registered");
            }

            // Create new user
            xyz.oiio.n8n.entity.User newUser = xyz.oiio.n8n.entity.User.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .email(registerRequest.getEmail())
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .disabled(false)
                    .mfaEnabled(false)
                    .role(xyz.oiio.n8n.entity.User.UserRole.USER)
                    .build();

            xyz.oiio.n8n.entity.User createdUser = userService.saveUser(newUser);

            // Auto-login after registration
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            registerRequest.getEmail(),
                            registerRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            // Set cookie
            addAuthCookie(response, jwt);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", createdUser.getId());
            userInfo.put("email", createdUser.getEmail());
            userInfo.put("firstName", createdUser.getFirstName());
            userInfo.put("lastName", createdUser.getLastName());
            userInfo.put("roles", List.of(createdUser.getRole().name().toLowerCase()));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("data", userInfo));
        } catch (Exception e) {
            log.error("Registration failed for user: {}", registerRequest.getEmail(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/owner/setup")
    public ResponseEntity<Map<String, Object>> setupOwner(@Valid @RequestBody RegisterRequest registerRequest,
            HttpServletResponse response) {
        try {
            if (userService.findByEmail(registerRequest.getEmail()).isPresent()) {
                throw new RuntimeException("Email is already registered");
            }

            // Create new owner user
            xyz.oiio.n8n.entity.User newUser = xyz.oiio.n8n.entity.User.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .email(registerRequest.getEmail())
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .disabled(false)
                    .mfaEnabled(false)
                    .role(xyz.oiio.n8n.entity.User.UserRole.OWNER)
                    .build();

            xyz.oiio.n8n.entity.User createdUser = userService.saveUser(newUser);

            // Auto-login after setup
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            registerRequest.getEmail(),
                            registerRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            // Set cookie
            addAuthCookie(response, jwt);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", createdUser.getId());
            userInfo.put("email", createdUser.getEmail());
            userInfo.put("firstName", createdUser.getFirstName());
            userInfo.put("lastName", createdUser.getLastName());
            userInfo.put("roles", List.of(createdUser.getRole().name().toLowerCase()));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("data", userInfo));
        } catch (Exception e) {
            log.error("Owner setup failed for: {}", registerRequest.getEmail(), e);
            throw new RuntimeException("Owner setup failed: " + e.getMessage());
        }
    }

    private void addAuthCookie(HttpServletResponse response, String token) {
        // Create cookie with name "n8n-auth"
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("n8n-auth", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 1 day
        // In production with HTTPS, you should uncomment this:
        // cookie.setSecure(true);

        response.addCookie(cookie);
    }

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("n8n Spring Boot Backend is running! API endpoints are at /rest");
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);

                if (tokenProvider.validateToken(token)) {
                    String email = tokenProvider.getUsernameFromToken(token);
                    Optional<xyz.oiio.n8n.entity.User> userOpt = userService.findByEmail(email);

                    if (userOpt.isPresent()) {
                        // Create authentication for token generation
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                email, null, null);

                        String newJwt = tokenProvider.generateToken(authentication);

                        Map<String, Object> userInfo = new HashMap<>();
                        xyz.oiio.n8n.entity.User user = userOpt.get();
                        userInfo.put("id", user.getId());
                        userInfo.put("email", user.getEmail());
                        userInfo.put("firstName", user.getFirstName());
                        userInfo.put("lastName", user.getLastName());
                        userInfo.put("roles", List.of(user.getRole().name().toLowerCase()));

                        return ResponseEntity
                                .ok(new AuthResponse(true, "Token refreshed successfully", newJwt, userInfo));
                    }
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "Invalid or expired token", null, null));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "Token refresh failed", null, null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        log.info("=== Logout request received ===");

        // Clear the n8n-auth cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("n8n-auth", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);

        // Clear security context
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // DTOs
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRequest {
        @JsonProperty("emailOrLdapLoginId")
        private String email;
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegisterRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthResponse {
        private boolean success;
        private String message;
        private String token;
        private Map<String, Object> user;
    }
}