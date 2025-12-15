package xyz.oiio.n8n.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.WebhookEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookRepository extends JpaRepository<WebhookEntity, WebhookEntity.WebhookId> {

    Optional<WebhookEntity> findByWebhookPathAndMethod(String webhookPath, String method);

    List<WebhookEntity> findByWebhookIdAndMethodAndPathLength(String webhookId, String method, Integer pathLength);

    List<WebhookEntity> findByWebhookIdAndPathLength(String webhookId, Integer pathLength);
}
