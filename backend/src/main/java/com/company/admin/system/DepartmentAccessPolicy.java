package com.company.admin.system;

import com.company.admin.common.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DepartmentAccessPolicy {

    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    public static final String DEFAULT_DEPARTMENT_ROLE = "DEPARTMENT_USER";

    private static final String PARTY_HR_ROLE = "PARTY_HR_USER";
    private static final String GENERAL_ADMIN_ROLE = "GENERAL_ADMIN_USER";

    private static final Map<String, DepartmentMenuAccess> DEPARTMENT_MENU_ACCESS = Map.of(
            "PARTY_HR", new DepartmentMenuAccess("/party-hr", "menu:party-hr"),
            "GENERAL_ADMIN", new DepartmentMenuAccess("/general-admin", "menu:general-admin"));

    private static final Map<String, String> DEPARTMENT_ROLE_CODES = Map.of(
            "PARTY_HR", PARTY_HR_ROLE,
            "GENERAL_ADMIN", GENERAL_ADMIN_ROLE);

    // 未来新增部门时，应优先扩展本策略；如果部门/菜单/角色变为运营配置，则升级为数据库配置。
    public Set<String> registerDepartmentCodes() {
        return DEPARTMENT_ROLE_CODES.keySet();
    }

    public String defaultDepartmentRoleCode() {
        return DEFAULT_DEPARTMENT_ROLE;
    }

    public Optional<String> departmentRoleCode(String departmentCode) {
        if (departmentCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(DEPARTMENT_ROLE_CODES.get(departmentCode));
    }

    public Optional<String> departmentMenuPermissionCode(String departmentCode) {
        if (departmentCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(DEPARTMENT_MENU_ACCESS.get(departmentCode))
                .map(DepartmentMenuAccess::permissionCode);
    }

    public Optional<String> departmentMenuPath(String departmentCode) {
        if (departmentCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(DEPARTMENT_MENU_ACCESS.get(departmentCode))
                .map(DepartmentMenuAccess::path);
    }

    public void validateRoleAssignment(User user, Set<String> roleCodes) {
        // 后端必须校验部门和角色兼容性，防止绕过菜单直接调用跨部门接口拿到越权能力。
        if (roleCodes.contains(PARTY_HR_ROLE) && roleCodes.contains(GENERAL_ADMIN_ROLE)) {
            throw new BusinessException("普通用户不能同时拥有多个部门细分角色");
        }

        String departmentCode = departmentCode(user);
        if (departmentCode == null) {
            validateSystemUserRoles(roleCodes);
            return;
        }

        Set<String> allowedRoleCodes = allowedRoleCodesForDepartment(departmentCode);
        List<String> invalidCodes = roleCodes.stream()
                .filter(code -> !allowedRoleCodes.contains(code))
                .toList();
        if (!invalidCodes.isEmpty()) {
            throw new BusinessException("用户所属部门不允许分配角色: " + String.join(",", invalidCodes));
        }
    }

    private Set<String> allowedRoleCodesForDepartment(String departmentCode) {
        return departmentRoleCode(departmentCode)
                .map(departmentRole -> {
                    Set<String> allowed = new LinkedHashSet<>();
                    allowed.add(DEFAULT_DEPARTMENT_ROLE);
                    allowed.add(departmentRole);
                    allowed.add(SUPER_ADMIN_ROLE);
                    return allowed;
                })
                .orElseGet(() -> Set.of(SUPER_ADMIN_ROLE));
    }

    private void validateSystemUserRoles(Set<String> roleCodes) {
        List<String> invalidCodes = roleCodes.stream()
                .filter(code -> !SUPER_ADMIN_ROLE.equals(code))
                .toList();
        if (!invalidCodes.isEmpty()) {
            throw new BusinessException("系统级用户不允许分配部门角色: " + String.join(",", invalidCodes));
        }
    }

    private String departmentCode(User user) {
        Department department = user.getDepartment();
        return department == null ? null : department.getCode();
    }

    private record DepartmentMenuAccess(String path, String permissionCode) {
    }
}
