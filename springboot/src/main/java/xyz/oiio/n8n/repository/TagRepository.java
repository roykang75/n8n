package xyz.oiio.n8n.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.TagEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Long> {

    boolean existsByName(String name);

    Optional<TagEntity> findByName(String name);

    @Query("SELECT t FROM TagEntity t JOIN t.workflows w WHERE w.owner.id = :userId")
    List<TagEntity> findTagsByUserId(String userId);

    @Query("SELECT t FROM TagEntity t JOIN t.workflows w WHERE w.project.id = :projectId")
    List<TagEntity> findTagsByProjectId(String projectId);

    @Query("SELECT t.name FROM TagEntity t JOIN t.workflows w WHERE w.id = :workflowId")
    List<String> findTagNamesByWorkflowId(String workflowId);
}