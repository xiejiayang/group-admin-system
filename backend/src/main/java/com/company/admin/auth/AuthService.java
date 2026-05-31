package com.company.admin.auth;

import com.company.admin.auth.dto.AuthResponse;
import com.company.admin.auth.dto.LoginRequest;
import com.company.admin.auth.dto.RegisterRequest;
import com.company.admin.common.BusinessException;
import com.company.admin.security.JwtService;
import com.company.admin.system.Department;
import com.company.admin.system.DepartmentRepository;
import com.company.admin.system.Permission;
import com.company.admin.system.Role;
import com.company.admin.system.RoleRepository;
import com.company.admin.system.User;
import com.company.admin.system.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DEFAULT_REGISTER_ROLE = "DEPARTMENT_USER";
    private static final Map<String, String> DEPARTMENT_ROLE_CODES = Map.of(
            "PARTY_HR", "PARTY_HR_USER",
            "GENERAL_ADMIN", "GENERAL_ADMIN_USER");
    private static final Set<String> REGISTER_DEPARTMENT_CODES = DEPARTMENT_ROLE_CODES.keySet();

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.username())
                .filter(this::isEnabled)
                .orElseThrow(this::badCredentials);

        // 登录只使用 PasswordEncoder.matches 校验 BCrypt 哈希，禁止把明文密码取出或直接比对。
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw badCredentials();
        }

        user.setLastLoginTime(LocalDateTime.now());
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!REGISTER_DEPARTMENT_CODES.contains(request.departmentCode())) {
            throw new BusinessException("部门只能选择党群人力部或综合管理部");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }

        Department department = departmentRepository.findByCodeAndEnabledTrue(request.departmentCode())
                .orElseThrow(() -> new BusinessException("部门不存在或已停用"));
        Role defaultRole = roleRepository.findByCodeAndEnabledTrue(DEFAULT_REGISTER_ROLE)
                .orElseThrow(() -> new BusinessException("默认角色不存在或已停用"));
        Role departmentRole = roleRepository.findByCodeAndEnabledTrue(DEPARTMENT_ROLE_CODES.get(request.departmentCode()))
                .orElseThrow(() -> new BusinessException("部门细分角色不存在或已停用"));

        User user = new User();
        user.setUsername(request.username());
        user.setPhone(request.phone());
        user.setDepartment(department);
        user.setStatus(User.STATUS_ENABLED);
        user.setDeleted(false);
        // 注册密码写入前必须 BCrypt 加密，数据库永远不保存明文密码。
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // 注册账号同时绑定基础部门角色和部门细分角色，避免 DEPARTMENT_USER 聚合出跨部门菜单权限。
        user.getRoles().add(defaultRole);
        user.getRoles().add(departmentRole);

        try {
            // 并发注册同名账号时，数据库唯一约束是最后兜底；这里统一转换成业务错误，避免返回 500。
            User savedUser = userRepository.saveAndFlush(user);
            return toAuthResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("用户名已存在");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse currentUser(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .filter(this::isEnabled)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录"));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .filter(Role::isEnabled)
                .map(Role::getCode)
                .sorted()
                .toList();

        // 当前用户权限由已启用角色聚合而来，返回给前端用于菜单和按钮级能力判断。
        List<String> permissions = user.getRoles().stream()
                .filter(Role::isEnabled)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        Department department = user.getDepartment();
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                department == null ? null : department.getCode(),
                department == null ? null : department.getName(),
                roles,
                permissions,
                jwtService.generateToken(user.getUsername()));
    }

    private boolean isEnabled(User user) {
        return User.STATUS_ENABLED.equals(user.getStatus());
    }

    private BusinessException badCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }
}
