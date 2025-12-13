package xyz.oiio.n8n.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import xyz.oiio.n8n.entity.User;

import java.util.Optional;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByDisabledFalse();

    @Query("SELECT u FROM User u WHERE u.mfaEnabled = :enabled AND u.disabled = false")
    List<User> findUsersWithMfaEnabled(boolean enabled);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.disabled = false")
    List<User> findByRole(User.UserRole role);
}