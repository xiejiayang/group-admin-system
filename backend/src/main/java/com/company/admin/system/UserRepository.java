package com.company.admin.system;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
    Optional<User> findByUsernameAndDeletedFalse(String username);

    @EntityGraph(attributePaths = {"department", "roles"})
    List<User> findByDeletedFalseOrderByIdAsc();

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"roles"})
    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.roles r
            WHERE u.deleted = false
              AND u.status = 'ENABLED'
              AND r.enabled = true
              AND r.code = :roleCode
            """)
    List<User> lockEnabledUsersWithRoleCode(@Param("roleCode") String roleCode);

    boolean existsByUsername(String username);
}
