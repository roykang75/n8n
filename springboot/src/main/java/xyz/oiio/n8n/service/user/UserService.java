package xyz.oiio.n8n.service.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.oiio.n8n.entity.User;
import xyz.oiio.n8n.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.UserRole.USER)
                .disabled(false)
                .mfaEnabled(false)
                .lastActiveAt(LocalDateTime.now())
                .settings(request.getSettings())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created new user with id: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(String userId, UserRequest request) {
        User user = getUserById(userId);

        if (!user.getEmail().equals(request.getEmail()) &&
            userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getSettings() != null) {
            user.setSettings(request.getSettings());
        }

        User savedUser = userRepository.save(user);
        log.info("Updated user with id: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public void deleteUser(String userId) {
        User user = getUserById(userId);
        // Soft delete
        user.setDisabled(true);
        userRepository.save(user);
        log.info("Soft deleted user with id: {}", userId);
    }

    @Transactional(readOnly = true)
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userRepository.findByDisabledFalse();
    }

    @Transactional
    public User enableMfa(String userId, String secret, List<String> recoveryCodes) {
        User user = getUserById(userId);
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        user.setMfaRecoveryCodes(recoveryCodes);

        User savedUser = userRepository.save(user);
        log.info("Enabled MFA for user with id: {}", userId);
        return savedUser;
    }

    @Transactional
    public User disableMfa(String userId) {
        User user = getUserById(userId);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaRecoveryCodes(null);

        User savedUser = userRepository.save(user);
        log.info("Disabled MFA for user with id: {}", userId);
        return savedUser;
    }

    @Transactional
    public User updateLastActive(String userId) {
        User user = getUserById(userId);
        user.setLastActiveAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserRequest {
        private String email;
        private String firstName;
        private String lastName;
        private String password;
        private User.UserRole role;
        private User.UserSettings settings;
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}