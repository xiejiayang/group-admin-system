package com.company.admin.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.admin.common.BusinessException;
import com.company.admin.system.dto.AssignRolesRequest;
import com.company.admin.system.dto.RoleResponse;
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

    @Mock
    private OperationLogService operationLogService;

    private SystemService systemService;

    @BeforeEach
    void setUp() {
        systemService = new SystemService(
                menuRepository,
                userRepository,
                roleRepository,
                new DepartmentAccessPolicy(),
                operationLogService);
    }

    @Test
    void assignRolesLocksSuperAdminsBeforeRejectingLastSuperAdminRemoval() {
        User superadmin = user(1L, "superadmin", role("SUPER_ADMIN"));

        when(userRepository.findByUsernameAndDeletedFalse("superadmin")).thenReturn(Optional.of(superadmin));
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(superadmin));
        when(userRepository.lockEnabledUsersWithRoleCode("SUPER_ADMIN")).thenReturn(List.of(superadmin));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> systemService.assignRoles("superadmin", 1L, new AssignRolesRequest(List.of("PARTY_HR_USER"))));

        assertThat(exception.getMessage()).isEqualTo("至少保留一个超级管理员");
        verify(userRepository).lockEnabledUsersWithRoleCode("SUPER_ADMIN");
        assertThat(superadmin.getRoles()).extracting(Role::getCode).containsExactly("SUPER_ADMIN");
    }

    @Test
    void partyHrAdminRolesForDepartmentTargetOnlyIncludePartyHrAdminAndUserRoles() {
        User operator = user(2L, "party_admin", department("PARTY_HR", "党群人力部"), role("PARTY_HR_ADMIN"));
        User target = user(3L, "party_user", department("PARTY_HR", "党群人力部"), role("PARTY_HR_USER"));

        when(userRepository.findByUsernameAndDeletedFalse("party_admin")).thenReturn(Optional.of(operator));
        when(userRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(target));
        when(roleRepository.findByEnabledTrueOrderByIdAsc()).thenReturn(List.of(
                role("SUPER_ADMIN"),
                role("PARTY_HR_ADMIN"),
                role("PARTY_HR_USER"),
                role("GENERAL_ADMIN_ADMIN"),
                role("GENERAL_ADMIN_USER")));

        List<RoleResponse> responses = systemService.roles("party_admin", 3L);

        assertThat(responses).extracting(RoleResponse::code)
                .containsExactly("PARTY_HR_ADMIN", "PARTY_HR_USER");
    }

    @Test
    void departmentUserCannotReadUsers() {
        User operator = user(4L, "party_user", department("PARTY_HR", "党群人力部"), role("PARTY_HR_USER"));

        when(userRepository.findByUsernameAndDeletedFalse("party_user")).thenReturn(Optional.of(operator));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> systemService.users("party_user"));

        assertThat(exception.getStatus().value()).isEqualTo(403);
    }

    @Test
    void assignRolesRecordsOperationLogAfterSaving() {
        User operator = user(5L, "superadmin", role("SUPER_ADMIN"));
        User target = user(6L, "party_user", department("PARTY_HR", "党群人力部"), role("PARTY_HR_USER"));
        Role partyHrAdmin = role("PARTY_HR_ADMIN");

        when(userRepository.findByUsernameAndDeletedFalse("superadmin")).thenReturn(Optional.of(operator));
        when(userRepository.findByIdAndDeletedFalse(6L)).thenReturn(Optional.of(target));
        when(roleRepository.findByCodeInAndEnabledTrue(any())).thenReturn(List.of(partyHrAdmin));

        systemService.assignRoles("superadmin", 6L, new AssignRolesRequest(List.of("PARTY_HR_ADMIN")));

        verify(operationLogService).recordRoleAssigned(operator, target, List.of(partyHrAdmin));
        assertThat(target.getRoles()).extracting(Role::getCode)
                .containsExactly("PARTY_HR_ADMIN");
    }

    @Test
    void partyHrAdminOperationLogsDelegateToOwnDepartment() {
        User operator = user(7L, "party_admin", department("PARTY_HR", "党群人力部"), role("PARTY_HR_ADMIN"));

        when(userRepository.findByUsernameAndDeletedFalse("party_admin")).thenReturn(Optional.of(operator));

        systemService.operationLogs("party_admin");

        verify(operationLogService).listByDepartment("党群人力部");
        verify(operationLogService, never()).listAll();
    }

    @Test
    void superAdminOperationLogsDelegateToAllLogs() {
        User operator = user(8L, "superadmin", role("SUPER_ADMIN"));

        when(userRepository.findByUsernameAndDeletedFalse("superadmin")).thenReturn(Optional.of(operator));

        systemService.operationLogs("superadmin");

        verify(operationLogService).listAll();
        verify(operationLogService, never()).listByDepartment(anyString());
    }

    private User user(Long id, String username, Role... roles) {
        return user(id, username, null, roles);
    }

    private User user(Long id, String username, Department department, Role... roles) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setRealName(username + "_real");
        user.setPhone("00000000000");
        user.setPasswordHash("encoded-password");
        user.setDepartment(department);
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

    private Department department(String code, String name) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "code", code);
        ReflectionTestUtils.setField(department, "name", name);
        ReflectionTestUtils.setField(department, "enabled", true);
        return department;
    }
}
