package xyz.oiio.n8n.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.Project;
import xyz.oiio.n8n.entity.User;
import xyz.oiio.n8n.entity.WorkflowEntity;

import java.util.List;
import java.util.Optional;

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
}