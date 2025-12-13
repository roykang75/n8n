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

    @EmbeddedId
    private UserAuthIdentityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Embeddable
    public static class UserAuthIdentityId implements java.io.Serializable {
        @Column(name = "user_id")
        private String userId;

        @Column(name = "id")
        private String id;
    }
}