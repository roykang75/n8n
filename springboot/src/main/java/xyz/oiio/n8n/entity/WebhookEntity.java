package xyz.oiio.n8n.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "webhooks")
@IdClass(WebhookEntity.WebhookId.class)
public class WebhookEntity {

    @Id
    @Column(name = "webhook_path", nullable = false)
    private String webhookPath;

    @Id
    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "node", nullable = false)
    private String node;

    @Column(name = "webhook_id")
    private String webhookId;

    @Column(name = "path_length")
    private Integer pathLength;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookId implements Serializable {
        private String webhookPath;
        private String method;
    }
}
