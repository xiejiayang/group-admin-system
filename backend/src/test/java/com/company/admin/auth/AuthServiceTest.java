package com.company.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.company.admin.auth.dto.LoginRequest;
import com.company.admin.auth.dto.RegisterRequest;
import com.company.admin.common.BusinessException;
import com.company.admin.security.JwtService;
import com.company.admin.system.Department;
import com.company.admin.system.DepartmentAccessPolicy;
import com.company.admin.system.DepartmentRepository;
import com.company.admin.system.OperationLogService;
import com.company.admin.system.Role;
import com.company.admin.system.RoleRepository;
import com.company.admin.system.User;
import com.company.admin.system.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private OperationLogService operationLogService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                departmentRepository,
                roleRepository,
                passwordEncoder,
                jwtService,
                new DepartmentAccessPolicy(),
                operationLogService);
    }

    @Test
    void registerSavesTrimmedRealName() {
        RegisterRequest request = new RegisterRequest(
                "register_user",
                "  Task Three User  ",
                "StrongPass123",
                "13600136002",
                "PARTY_HR");
        Role departmentRole = role("PARTY_HR_USER");

        when(userRepository.existsByUsername("register_user")).thenReturn(false);
        when(departmentRepository.findByCodeAndEnabledTrue("PARTY_HR")).thenReturn(Optional.of(new Department()));
        when(roleRepository.findByCodeAndEnabledTrue("PARTY_HR_USER")).thenReturn(Optional.of(departmentRole));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("register_user")).thenReturn("token");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getRealName()).isEqualTo("Task Three User");
        assertThat(userCaptor.getValue().getRoles())
                .extracting(Role::getCode)
                .containsExactly("PARTY_HR_USER");
        verify(roleRepository, never()).findByCodeAndEnabledTrue("DEPARTMENT_USER");
    }

    @Test
    void loginRecordsOperationLogAfterPasswordMatches() {
        User user = new User();
        user.setUsername("login_user");
        user.setRealName("Login Real Name");
        user.setPasswordHash("encoded-password");
        user.setStatus(User.STATUS_ENABLED);
        user.getRoles().add(role("PARTY_HR_USER"));

        when(userRepository.findByUsernameAndDeletedFalse("login_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("login_user")).thenReturn("token");

        authService.login(new LoginRequest("login_user", "plain-password"));

        verify(operationLogService).recordLogin(user);
    }

    @Test
    void registerMapsDatabaseUsernameUniqueConflictToBusinessException() {
        RegisterRequest request = new RegisterRequest(
                "race_user",
                "Race User",
                "StrongPass123",
                "13600136000",
                "PARTY_HR");

        when(userRepository.existsByUsername("race_user")).thenReturn(false);
        when(departmentRepository.findByCodeAndEnabledTrue("PARTY_HR")).thenReturn(Optional.of(new Department()));
        when(roleRepository.findByCodeAndEnabledTrue("PARTY_HR_USER")).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_sys_user_username"));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));

        assertThat(exception.getMessage()).isEqualTo("用户名已存在");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void registerDoesNotMapOtherIntegrityErrorsToDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "valid_user",
                "Valid User",
                "StrongPass123",
                "13600136001",
                "PARTY_HR");

        when(userRepository.existsByUsername("valid_user")).thenReturn(false);
        when(departmentRepository.findByCodeAndEnabledTrue("PARTY_HR")).thenReturn(Optional.of(new Department()));
        when(roleRepository.findByCodeAndEnabledTrue("PARTY_HR_USER")).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("value too long for column phone"));

        DataIntegrityViolationException exception =
                assertThrows(DataIntegrityViolationException.class, () -> authService.register(request));

        assertThat(exception.getMessage()).contains("value too long");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    private Role role(String code) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", code);
        return role;
    }
}
