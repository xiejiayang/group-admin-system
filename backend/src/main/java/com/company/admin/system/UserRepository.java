package com.company.admin.system;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
    Optional<User> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsername(String username);
}
