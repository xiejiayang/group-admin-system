package com.company.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.admin.auth.dto.RegisterRequest;
import com.company.admin.common.BusinessException;
import com.company.admin.security.JwtService;
import com.company.admin.system.Department;
import com.company.admin.system.DepartmentRepository;
import com.company.admin.system.Role;
import com.company.admin.system.RoleRepository;
import com.company.admin.system.User;
import com.company.admin.system.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                departmentRepository,
                roleRepository,
                passwordEncoder,
                jwtService);
    }

    @Test
    void registerMapsDatabaseUsernameUniqueConflictToBusinessException() {
        RegisterRequest request = new RegisterRequest(
                "race_user",
                "StrongPass123",
                "13600136000",
                "PARTY_HR");

        when(userRepository.existsByUsername("race_user")).thenReturn(false);
        when(departmentRepository.findByCodeAndEnabledTrue("PARTY_HR")).thenReturn(Optional.of(new Department()));
        when(roleRepository.findByCodeAndEnabledTrue("DEPARTMENT_USER")).thenReturn(Optional.of(new Role()));
        when(roleRepository.findByCodeAndEnabledTrue("PARTY_HR_USER")).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_sys_user_username"));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));

        assertThat(exception.getMessage()).isEqualTo("用户名已存在");
        verify(userRepository).saveAndFlush(any(User.class));
    }
}
