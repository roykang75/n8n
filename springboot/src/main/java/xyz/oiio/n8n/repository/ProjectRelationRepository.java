package xyz.oiio.n8n.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.ProjectRelation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRelationRepository extends JpaRepository<ProjectRelation, Long> {

    @Query("SELECT pr FROM ProjectRelation pr WHERE pr.user.id = :userId AND pr.project.id = :projectId")
    Optional<ProjectRelation> findByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") Long projectId);

    List<ProjectRelation> findByUserId(String userId);

    List<ProjectRelation> findByProjectId(Long projectId);

    List<ProjectRelation> findByProjectIdAndRole(Long projectId, ProjectRelation.RelationRole role);

    boolean existsByUserIdAndProjectIdAndRole(String userId, Long projectId, ProjectRelation.RelationRole role);
}