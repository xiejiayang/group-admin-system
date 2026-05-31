package com.company.admin.system;

import com.company.admin.common.BusinessException;
import com.company.admin.system.dto.AssignRolesRequest;
import com.company.admin.system.dto.MenuResponse;
import com.company.admin.system.dto.RoleResponse;
import com.company.admin.system.dto.UserResponse;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final Map<String, String> DEPARTMENT_MENU_PERMISSIONS = Map.of(
            "PARTY_HR", "menu:party-hr",
            "GENERAL_ADMIN", "menu:general-admin");

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public SystemService(
            MenuRepository menuRepository,
            UserRepository userRepository,
            RoleRepository roleRepository) {
        this.menuRepository = menuRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> currentUserMenus(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .filter(this::isEnabled)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录"));

        Set<String> permissionCodes = enabledPermissionCodes(user);
        Set<String> visiblePermissionCodes = visibleMenuPermissionCodes(user, permissionCodes);
        if (visiblePermissionCodes.isEmpty()) {
            return List.of();
        }

        return menuRepository.findByPermissionCodeInAndEnabledTrueOrderBySortOrderAsc(visiblePermissionCodes)
                .stream()
                .map(this::toMenuResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        return userRepository.findByDeletedFalseOrderByIdAsc().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findByEnabledTrueOrderByIdAsc().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional
    public UserResponse assignRoles(Long userId, AssignRolesRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        Set<String> requestedCodes = request.roleCodes().stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Role> roles = requestedCodes.isEmpty()
                ? List.of()
                : roleRepository.findByCodeInAndEnabledTrue(requestedCodes);
        Map<String, Role> rolesByCode = roles.stream()
                .collect(Collectors.toMap(Role::getCode, Function.identity()));
        List<String> missingCodes = requestedCodes.stream()
                .filter(code -> !rolesByCode.containsKey(code))
                .toList();
        if (!missingCodes.isEmpty()) {
            throw new BusinessException("角色不存在或已停用: " + String.join(",", missingCodes));
        }

        boolean removingSuperAdmin = hasRole(user, SUPER_ADMIN_ROLE) && !requestedCodes.contains(SUPER_ADMIN_ROLE);
        // 超级管理员保底规则：禁止移除系统中最后一个 SUPER_ADMIN，避免系统无人可管理。
        if (removingSuperAdmin && userRepository.countEnabledUsersWithRoleCode(SUPER_ADMIN_ROLE) <= 1) {
            throw new BusinessException("至少保留一个超级管理员");
        }

        user.getRoles().clear();
        requestedCodes.stream()
                .map(rolesByCode::get)
                .forEach(user.getRoles()::add);
        return toUserResponse(user);
    }

    private Set<String> visibleMenuPermissionCodes(User user, Set<String> permissionCodes) {
        if (hasRole(user, SUPER_ADMIN_ROLE)) {
            return permissionCodes;
        }

        Department department = user.getDepartment();
        String departmentCode = department == null ? null : department.getCode();
        String departmentMenuPermission = DEPARTMENT_MENU_PERMISSIONS.get(departmentCode);
        if (departmentMenuPermission == null || !permissionCodes.contains(departmentMenuPermission)) {
            return Set.of();
        }

        // 菜单以“当前用户权限 + 所属部门”裁剪，避免跨部门菜单泄露。
        return Set.of(departmentMenuPermission);
    }

    private Set<String> enabledPermissionCodes(User user) {
        return user.getRoles().stream()
                .filter(Role::isEnabled)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream()
                .filter(Role::isEnabled)
                .map(Role::getCode)
                .anyMatch(roleCode::equals);
    }

    private boolean isEnabled(User user) {
        return User.STATUS_ENABLED.equals(user.getStatus());
    }

    private MenuResponse toMenuResponse(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPath(),
                menu.getPermissionCode(),
                menu.getSortOrder());
    }

    private UserResponse toUserResponse(User user) {
        Department department = user.getDepartment();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                department == null ? null : department.getCode(),
                department == null ? null : department.getName(),
                user.getRoles().stream()
                        .filter(Role::isEnabled)
                        .map(Role::getCode)
                        .sorted(Comparator.naturalOrder())
                        .toList(),
                user.getStatus());
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName());
    }
}
