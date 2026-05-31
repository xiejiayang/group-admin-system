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
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Set<String> REGISTER_DEPARTMENT_CODES = Set.of("PARTY_HR", "GENERAL_ADMIN");
    private static final String DEFAULT_REGISTER_ROLE = "DEPARTMENT_USER";

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

        User user = new User();
        user.setUsername(request.username());
        user.setPhone(request.phone());
        user.setDepartment(department);
        user.setStatus(User.STATUS_ENABLED);
        user.setDeleted(false);
        // 注册密码写入前必须 BCrypt 加密，数据库永远不保存明文密码。
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // 第一版注册账号固定绑定 DEPARTMENT_USER，后续细分授权时再扩展角色选择流程。
        user.getRoles().add(defaultRole);

        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
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
