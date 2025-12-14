package xyz.oiio.n8n.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36) NOT NULL")
    private String id;

    @Email
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "first_name", length = 32)
    private String firstName;

    @Column(name = "last_name", length = 32)
    private String lastName;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "disabled", nullable = false)
    @Builder.Default
    private Boolean disabled = false;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mfa_recovery_codes", columnDefinition = "JSON")
    private List<String> mfaRecoveryCodes;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "JSON")
    private UserSettings settings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personalization_answers", columnDefinition = "JSON")
    private Object personalizationAnswers;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAuthIdentity> authIdentities;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowEntity> workflows;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CredentialsEntity> credentials;

    public enum UserRole {
        ADMIN,
        USER,
        OWNER
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSettings {
        private String theme;
        private String language;
        private Integer itemsPerPage;
        private Boolean autoSave;
        private Boolean receiveEmailUpdates;
        private Boolean easyAIWorkflowOnboarded;
    }
}