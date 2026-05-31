package com.company.admin.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.admin.common.BusinessException;
import com.company.admin.system.dto.AssignRolesRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private SystemService systemService;

    @BeforeEach
    void setUp() {
        systemService = new SystemService(
                menuRepository,
                userRepository,
                roleRepository,
                new DepartmentAccessPolicy());
    }

    @Test
    void assignRolesLocksSuperAdminsBeforeRejectingLastSuperAdminRemoval() {
        User superadmin = user(1L, "superadmin", role("SUPER_ADMIN"));
        Role departmentRole = role("DEPARTMENT_USER");

        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(superadmin));
        when(roleRepository.findByCodeInAndEnabledTrue(any())).thenReturn(List.of(departmentRole));
        when(userRepository.lockEnabledUsersWithRoleCode("SUPER_ADMIN")).thenReturn(List.of(superadmin));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> systemService.assignRoles(1L, new AssignRolesRequest(List.of("DEPARTMENT_USER"))));

        assertThat(exception.getMessage()).isEqualTo("至少保留一个超级管理员");
        verify(userRepository).lockEnabledUsersWithRoleCode("SUPER_ADMIN");
        assertThat(superadmin.getRoles()).extracting(Role::getCode).containsExactly("SUPER_ADMIN");
    }

    private User user(Long id, String username, Role... roles) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setPhone("00000000000");
        user.setPasswordHash("encoded-password");
        user.setStatus(User.STATUS_ENABLED);
        user.setDeleted(false);
        user.getRoles().addAll(List.of(roles));
        return user;
    }

    private Role role(String code) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", code);
        ReflectionTestUtils.setField(role, "name", code);
        ReflectionTestUtils.setField(role, "enabled", true);
        return role;
    }
}
