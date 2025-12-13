package xyz.oiio.n8n.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "credentials")
public class CredentialsEntity extends BaseTimeEntity {

    @Id
    @GenericGenerator(name = "nanoid", strategy = "xyz.oiio.n8n.util.NanoIdGenerator")
    @GeneratedValue(generator = "nanoid")
    @Column(name = "id", columnDefinition = "VARCHAR(21) NOT NULL")
    private String id;

    @Size(max = 128)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String data; // Encrypted credential data

    @Column(name = "type", nullable = false, length = 128)
    private String type;

    @Column(name = "is_managed", nullable = false)
    @Builder.Default
    private Boolean isManaged = false;

    @Column(name = "is_global", nullable = false)
    @Builder.Default
    private Boolean isGlobal = false;

    @Column(name = "is_resolvable", nullable = false)
    @Builder.Default
    private Boolean isResolvable = false;

    @Column(name = "resolvable_allow_fallback", nullable = false)
    @Builder.Default
    private Boolean resolvableAllowFallback = false;

    @Column(name = "resolver_id")
    private String resolverId;

    @Column(name = "project_id", columnDefinition = "BIGINT")
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = true)
    @JsonIgnore
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Project project;

    @ManyToMany
    @JoinTable(
        name = "shared_credentials",
        joinColumns = @JoinColumn(name = "credentials_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> sharedWith;
}