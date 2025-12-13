package xyz.oiio.n8n.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.CredentialsEntity;

import java.util.List;

@Repository
public interface CredentialsRepository extends JpaRepository<CredentialsEntity, String> {

    List<CredentialsEntity> findByOwnerIdAndIsGlobalFalse(String ownerId);

    List<CredentialsEntity> findByProjectIdAndIsGlobalFalse(Long projectId);

    List<CredentialsEntity> findByIsGlobalTrue();

    List<CredentialsEntity> findByType(String type);

    @Query("SELECT c FROM CredentialsEntity c WHERE " +
           "(c.owner.id = :userId OR c.isGlobal = true OR c.project.id IN " +
           "(SELECT p.id FROM Project p JOIN p.relations r WHERE r.user.id = :userId))")
    List<CredentialsEntity> findAccessibleCredentials(@Param("userId") String userId);

    @Query("SELECT c FROM CredentialsEntity c WHERE c.type = :type AND " +
           "(c.owner.id = :userId OR c.isGlobal = true OR c.project.id IN " +
           "(SELECT p.id FROM Project p JOIN p.relations r WHERE r.user.id = :userId))")
    List<CredentialsEntity> findAccessibleCredentialsByType(@Param("userId") String userId, @Param("type") String type);

    boolean existsByNameAndOwnerId(String name, String ownerId);

    @Query("SELECT DISTINCT c.type FROM CredentialsEntity c WHERE " +
           "(c.owner.id = :userId OR c.isGlobal = true OR c.project.id IN " +
           "(SELECT p.id FROM Project p JOIN p.relations r WHERE r.user.id = :userId))")
    List<String> findDistinctCredentialTypes(@Param("userId") String userId);
}