package xyz.oiio.n8n.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.ExecutionEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionEntity, String> {

    Page<ExecutionEntity> findByWorkflowId(String workflowId, Pageable pageable);

    Page<ExecutionEntity> findByWorkflowIdAndDeletedAtIsNull(String workflowId, Pageable pageable);

    Page<ExecutionEntity> findByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);

    @Query("SELECT e FROM ExecutionEntity e WHERE e.workflow.id = :workflowId AND e.status = :status AND e.deletedAt IS NULL")
    List<ExecutionEntity> findByWorkflowIdAndStatus(@Param("workflowId") String workflowId, @Param("status") ExecutionEntity.ExecutionStatus status);

    @Query("SELECT e FROM ExecutionEntity e WHERE e.user.id = :userId AND e.startedAt BETWEEN :startDate AND :endDate AND e.deletedAt IS NULL")
    Page<ExecutionEntity> findByUserIdAndDateRange(@Param("userId") String userId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  Pageable pageable);

    @Query("SELECT e FROM ExecutionEntity e WHERE e.retryOf = :executionId AND e.deletedAt IS NULL")
    List<ExecutionEntity> findRetryExecutions(@Param("executionId") String executionId);

    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.workflow.id = :workflowId AND e.status = :status AND e.deletedAt IS NULL")
    long countByWorkflowIdAndStatus(@Param("workflowId") String workflowId, @Param("status") ExecutionEntity.ExecutionStatus status);

    long countByWorkflowIdAndDeletedAtIsNullAndStatus(String workflowId, ExecutionEntity.ExecutionStatus status);
}