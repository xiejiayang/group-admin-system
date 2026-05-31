package com.company.admin.system;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByPermissionCodeInAndEnabledTrueOrderBySortOrderAsc(Collection<String> permissionCodes);
}
