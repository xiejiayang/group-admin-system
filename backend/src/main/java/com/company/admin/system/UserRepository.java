package com.company.admin.system;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
    Optional<User> findByUsernameAndDeletedFalse(String username);

    @EntityGraph(attributePaths = {"department", "roles"})
    List<User> findByDeletedFalseOrderByIdAsc();

    @EntityGraph(attributePaths = {"department", "roles"})
    Optional<User> findByIdAndDeletedFalse(Long id);

    @Query("""
            SELECT COUNT(DISTINCT u.id)
            FROM User u
            JOIN u.roles r
            WHERE u.deleted = false
              AND u.status = 'ENABLED'
              AND r.enabled = true
              AND r.code = :roleCode
            """)
    long countEnabledUsersWithRoleCode(@Param("roleCode") String roleCode);

    boolean existsByUsername(String username);
}
