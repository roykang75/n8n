package xyz.oiio.n8n.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByType(Project.ProjectType type);

    @Query("SELECT p FROM Project p JOIN p.relations r WHERE r.user.id = :userId AND p.type = :type")
    List<Project> findProjectsByUserIdAndType(String userId, Project.ProjectType type);

    @Query("SELECT p FROM Project p JOIN p.relations r WHERE r.user.id = :userId")
    List<Project> findProjectsByUserId(String userId);

    boolean existsByName(String name);
}