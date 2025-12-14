package xyz.oiio.n8n.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.User;
import xyz.oiio.n8n.entity.WorkflowEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {

    Page<WorkflowEntity> findByOwnerAndIsArchivedFalse(User owner, Pageable pageable);

    Page<WorkflowEntity> findByProjectIdAndIsArchivedFalse(Long projectId, Pageable pageable);

    List<WorkflowEntity> findByOwnerAndIsArchivedFalse(User owner);

    List<WorkflowEntity> findByProjectIdAndIsArchivedFalse(Long projectId);

    List<WorkflowEntity> findByIsArchivedFalse();

    @Query("SELECT w FROM WorkflowEntity w JOIN w.tags t WHERE t.name = :tagName AND w.isArchived = false")
    List<WorkflowEntity> findByTagName(@Param("tagName") String tagName);

    @Query("SELECT w FROM WorkflowEntity w WHERE w.owner.id = :userId OR w.projectId IN " +
            "(SELECT p.id FROM Project p JOIN p.relations r WHERE r.user.id = :userId) " +
            "AND w.isArchived = false")
    Page<WorkflowEntity> findAccessibleWorkflows(@Param("userId") String userId, Pageable pageable);

    boolean existsByNameAndOwner(String name, User owner);

    /**
     * Direct UPDATE query to update workflow fields without loading/merging entity.
     * This avoids StaleObjectStateException during concurrent updates.
     * Similar to TypeORM's repository.update(id, payload) pattern.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkflowEntity w SET " +
            "w.name = COALESCE(:name, w.name), " +
            "w.description = COALESCE(:description, w.description), " +
            "w.nodes = COALESCE(:nodes, w.nodes), " +
            "w.connections = COALESCE(:connections, w.connections), " +
            "w.settings = COALESCE(:settings, w.settings), " +
            "w.staticData = COALESCE(:staticData, w.staticData), " +
            "w.meta = COALESCE(:meta, w.meta), " +
            "w.pinData = COALESCE(:pinData, w.pinData), " +
            "w.versionId = :versionId, " +
            "w.versionCounter = w.versionCounter + 1, " +
            "w.updatedAt = :updatedAt " +
            "WHERE w.id = :workflowId")
    int updateWorkflowFields(
            @Param("workflowId") String workflowId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("nodes") List<Object> nodes,
            @Param("connections") Object connections,
            @Param("settings") WorkflowEntity.WorkflowSettings settings,
            @Param("staticData") Object staticData,
            @Param("meta") WorkflowEntity.WorkflowMeta meta,
            @Param("pinData") Object pinData,
            @Param("versionId") String versionId,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Update only active status without loading entity.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkflowEntity w SET w.active = :active, w.updatedAt = :updatedAt WHERE w.id = :workflowId")
    int updateActiveStatus(
            @Param("workflowId") String workflowId,
            @Param("active") Boolean active,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Update archive status without loading entity.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkflowEntity w SET w.isArchived = :isArchived, w.updatedAt = :updatedAt WHERE w.id = :workflowId")
    int updateArchivedStatus(
            @Param("workflowId") String workflowId,
            @Param("isArchived") Boolean isArchived,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Increment trigger count without loading entity.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkflowEntity w SET w.triggerCount = w.triggerCount + 1 WHERE w.id = :workflowId")
    int incrementTriggerCount(@Param("workflowId") String workflowId);
}