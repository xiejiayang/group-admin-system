package com.company.admin.system;

import com.company.admin.common.BusinessException;
import com.company.admin.system.dto.AssignRolesRequest;
import com.company.admin.system.dto.MenuResponse;
import com.company.admin.system.dto.OperationLogResponse;
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

    private static final String PARTY_HR_DEPARTMENT = "PARTY_HR";
    private static final String GENERAL_ADMIN_DEPARTMENT = "GENERAL_ADMIN";
    private static final String SETTINGS_MENU_PERMISSION = "menu:settings";
    private static final String BOOTSTRAP_SUPERADMIN_USERNAME = "superadmin";

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentAccessPolicy departmentAccessPolicy;
    private final OperationLogService operationLogService;

    public SystemService(
            MenuRepository menuRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentAccessPolicy departmentAccessPolicy,
            OperationLogService operationLogService) {
        this.menuRepository = menuRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentAccessPolicy = departmentAccessPolicy;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> currentUserMenus(String username) {
        User user = currentOperator(username);

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
    public List<UserResponse> users(String operatorUsername) {
        User operator = currentOperator(operatorUsername);
        List<User> users = userRepository.findByDeletedFalseOrderByIdAsc();
        if (hasRole(operator, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)) {
            return users.stream()
                    .map(this::toUserResponse)
                    .toList();
        }

        String managedDepartmentCode = managedDepartmentCode(operator);
        if (managedDepartmentCode == null) {
            throw forbidden();
        }

        return users.stream()
                .filter(user -> managedDepartmentCode.equals(departmentCode(user)))
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles(String operatorUsername, Long targetUserId) {
        User operator = currentOperator(operatorUsername);
        User target = targetUser(targetUserId);
        ensureCanManageTarget(operator, target);

        Set<String> visibleRoleCodes = isSuperAdminTarget(target)
                ? Set.of(DepartmentAccessPolicy.SUPER_ADMIN_ROLE)
                : departmentAccessPolicy.visibleAssignableRoleCodes(target);

        return roleRepository.findByEnabledTrueOrderByIdAsc().stream()
                .filter(role -> visibleRoleCodes.contains(role.getCode()))
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional
    public UserResponse assignRoles(String operatorUsername, Long userId, AssignRolesRequest request) {
        User operator = currentOperator(operatorUsername);
        User user = targetUser(userId);
        ensureCanManageTarget(operator, user);

        Set<String> requestedCodes = request.roleCodes().stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean removingSuperAdmin = isEnabled(user)
                && hasRole(user, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)
                && !requestedCodes.contains(DepartmentAccessPolicy.SUPER_ADMIN_ROLE);
        if (removingSuperAdmin) {
            List<User> lockedSuperAdmins =
                    userRepository.lockEnabledUsersWithRoleCode(DepartmentAccessPolicy.SUPER_ADMIN_ROLE);
            // 移除超级管理员前必须在同一事务内锁定现有超级管理员，避免并发保存导致系统无人可管。
            if (lockedSuperAdmins.size() <= 1) {
                throw new BusinessException("至少保留一个超级管理员");
            }
        }

        if (isSuperAdminTarget(user) && !Set.of(DepartmentAccessPolicy.SUPER_ADMIN_ROLE).equals(requestedCodes)) {
            throw new BusinessException("超级管理员账号只允许分配超级管理员角色");
        }
        validateRequestedRoleCodes(user, requestedCodes);

        Set<String> finalRoleCodes = finalRoleCodes(user, requestedCodes);
        List<Role> roles = roleRepository.findByCodeInAndEnabledTrue(finalRoleCodes);
        Map<String, Role> rolesByCode = roles.stream()
                .collect(Collectors.toMap(Role::getCode, Function.identity()));
        List<String> missingCodes = finalRoleCodes.stream()
                .filter(code -> !rolesByCode.containsKey(code))
                .toList();
        if (!missingCodes.isEmpty()) {
            throw new BusinessException("角色不存在或已停用: " + String.join(",", missingCodes));
        }
        departmentAccessPolicy.validateRoleAssignment(user, finalRoleCodes);

        user.getRoles().clear();
        finalRoleCodes.stream()
                .map(rolesByCode::get)
                .forEach(user.getRoles()::add);
        operationLogService.recordRoleAssigned(operator, user, roles);
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<OperationLogResponse> operationLogs(String operatorUsername) {
        User operator = currentOperator(operatorUsername);
        if (hasRole(operator, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)) {
            return operationLogService.listAll();
        }

        if (isDepartmentAdmin(operator)) {
            return operationLogService.listByDepartment(departmentName(operator));
        }

        throw forbidden();
    }

    private Set<String> visibleMenuPermissionCodes(User user, Set<String> permissionCodes) {
        if (hasRole(user, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)) {
            return permissionCodes;
        }

        Set<String> visiblePermissionCodes = new LinkedHashSet<>();
        String departmentMenuPermission = departmentAccessPolicy.departmentMenuPermissionCode(departmentCode(user))
                .orElse(null);
        if (departmentMenuPermission != null && permissionCodes.contains(departmentMenuPermission)) {
            visiblePermissionCodes.add(departmentMenuPermission);
        }

        // 部门管理员除了本部门业务菜单，还能看到设置入口；普通部门用户即使伪造请求也不会获得设置菜单。
        if (isDepartmentAdmin(user) && permissionCodes.contains(SETTINGS_MENU_PERMISSION)) {
            visiblePermissionCodes.add(SETTINGS_MENU_PERMISSION);
        }
        return visiblePermissionCodes;
    }

    private void validateRequestedRoleCodes(User target, Set<String> requestedCodes) {
        // 前端只提交弹窗可见角色；隐藏的 DEPARTMENT_USER 由后端补齐，不允许客户端直接提交。
        Set<String> allowedRoleCodes = isSuperAdminTarget(target)
                ? Set.of(DepartmentAccessPolicy.SUPER_ADMIN_ROLE)
                : departmentAccessPolicy.visibleAssignableRoleCodes(target);
        List<String> invalidCodes = requestedCodes.stream()
                .filter(code -> !allowedRoleCodes.contains(code))
                .toList();
        if (!invalidCodes.isEmpty()) {
            throw new BusinessException("用户所属部门不允许分配角色: " + String.join(",", invalidCodes));
        }
    }

    private Set<String> finalRoleCodes(User target, Set<String> requestedCodes) {
        Set<String> finalRoleCodes = new LinkedHashSet<>(requestedCodes);
        // 部门账号必须始终保留基础部门角色，避免设置弹窗隐藏该角色后保存时误清空基础身份。
        if (!isSuperAdminTarget(target) && departmentCode(target) != null) {
            finalRoleCodes.add(DepartmentAccessPolicy.DEFAULT_DEPARTMENT_ROLE);
        }
        return finalRoleCodes;
    }

    private void ensureCanManageTarget(User operator, User target) {
        if (hasRole(operator, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)) {
            return;
        }
        String managedDepartmentCode = managedDepartmentCode(operator);
        if (managedDepartmentCode != null
                && !isSuperAdminTarget(target)
                && managedDepartmentCode.equals(departmentCode(target))) {
            return;
        }
        throw forbidden();
    }

    private String managedDepartmentCode(User operator) {
        if (PARTY_HR_DEPARTMENT.equals(departmentCode(operator))
                && hasRole(operator, DepartmentAccessPolicy.PARTY_HR_ADMIN_ROLE)) {
            return PARTY_HR_DEPARTMENT;
        }
        if (GENERAL_ADMIN_DEPARTMENT.equals(departmentCode(operator))
                && hasRole(operator, DepartmentAccessPolicy.GENERAL_ADMIN_MANAGER_ROLE)) {
            return GENERAL_ADMIN_DEPARTMENT;
        }
        return null;
    }

    private boolean isDepartmentAdmin(User user) {
        return managedDepartmentCode(user) != null;
    }

    private boolean isSuperAdminTarget(User user) {
        return BOOTSTRAP_SUPERADMIN_USERNAME.equals(user.getUsername())
                || hasRole(user, DepartmentAccessPolicy.SUPER_ADMIN_ROLE);
    }

    private User currentOperator(String username) {
        return userRepository.findByUsernameAndDeletedFalse(username)
                .filter(this::isEnabled)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录"));
    }

    private User targetUser(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
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

    private String departmentCode(User user) {
        Department department = user.getDepartment();
        return department == null ? null : department.getCode();
    }

    private String departmentName(User user) {
        Department department = user.getDepartment();
        return department == null ? null : department.getName();
    }

    private BusinessException forbidden() {
        return new BusinessException(HttpStatus.FORBIDDEN, "无权访问该资源");
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
                user.getRealName(),
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
