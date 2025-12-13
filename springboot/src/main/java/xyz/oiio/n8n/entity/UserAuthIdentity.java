package xyz.oiio.n8n.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "user_auth_identities")
public class UserAuthIdentity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "provider_type", nullable = false)
    private String providerType;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_data", columnDefinition = "JSON")
    private Object providerData;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;
}