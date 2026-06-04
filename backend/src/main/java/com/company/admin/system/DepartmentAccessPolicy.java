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
    public static final String PARTY_HR_ADMIN_ROLE = "PARTY_HR_ADMIN";
    public static final String GENERAL_ADMIN_MANAGER_ROLE = "GENERAL_ADMIN_MANAGER";
    public static final String PARTY_HR_USER = "PARTY_HR_USER";
    public static final String GENERAL_ADMIN_USER = "GENERAL_ADMIN_USER";

    private static final String PARTY_HR_DEPARTMENT = "PARTY_HR";
    private static final String GENERAL_ADMIN_DEPARTMENT = "GENERAL_ADMIN";

    private static final Map<String, DepartmentMenuAccess> DEPARTMENT_MENU_ACCESS = Map.of(
            PARTY_HR_DEPARTMENT, new DepartmentMenuAccess("/party-hr", "menu:party-hr"),
            GENERAL_ADMIN_DEPARTMENT, new DepartmentMenuAccess("/general-admin", "menu:general-admin"));

    private static final Map<String, String> DEPARTMENT_ROLE_CODES = Map.of(
            PARTY_HR_DEPARTMENT, PARTY_HR_USER,
            GENERAL_ADMIN_DEPARTMENT, GENERAL_ADMIN_USER);

    private static final Map<String, Set<String>> DEPARTMENT_ASSIGNABLE_ROLE_CODES = Map.of(
            PARTY_HR_DEPARTMENT, linkedRoleSet(DEFAULT_DEPARTMENT_ROLE, PARTY_HR_ADMIN_ROLE, PARTY_HR_USER),
            GENERAL_ADMIN_DEPARTMENT,
                    linkedRoleSet(DEFAULT_DEPARTMENT_ROLE, GENERAL_ADMIN_MANAGER_ROLE, GENERAL_ADMIN_USER));

    // 目前部门、菜单、角色仍是固定业务域；后续新增部门时，应在这里同步扩展映射，避免配置分散。
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
        // 角色分配必须以后端目标用户所属部门为准，防止绕过前端菜单后混用跨部门管理员或用户角色。
        Set<String> allowedRoleCodes = assignableRoleCodes(user);
        List<String> invalidCodes = roleCodes.stream()
                .filter(code -> !allowedRoleCodes.contains(code))
                .toList();
        if (!invalidCodes.isEmpty()) {
            throw new BusinessException("用户所属部门不允许分配角色: " + String.join(",", invalidCodes));
        }
    }

    public Set<String> assignableRoleCodes(User user) {
        String departmentCode = departmentCode(user);
        if (departmentCode == null) {
            return Set.of(SUPER_ADMIN_ROLE);
        }
        return DEPARTMENT_ASSIGNABLE_ROLE_CODES.getOrDefault(departmentCode, Set.of());
    }

    public Set<String> visibleAssignableRoleCodes(User user) {
        Set<String> roleCodes = new LinkedHashSet<>(assignableRoleCodes(user));
        roleCodes.remove(DEFAULT_DEPARTMENT_ROLE);
        return roleCodes;
    }

    private String departmentCode(User user) {
        Department department = user.getDepartment();
        return department == null ? null : department.getCode();
    }

    private static Set<String> linkedRoleSet(String... roleCodes) {
        return new LinkedHashSet<>(List.of(roleCodes));
    }

    private record DepartmentMenuAccess(String path, String permissionCode) {
    }
}
