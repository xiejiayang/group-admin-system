package com.company.admin.system;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByCodeAndEnabledTrue(String code);

    List<Role> findByEnabledTrueOrderByIdAsc();

    List<Role> findByCodeInAndEnabledTrue(Collection<String> codes);
}
