package com.company.admin.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.admin.system.dto.OperationLogResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-04T02:03:50Z"), DISPLAY_ZONE);
    private static final LocalDateTime FIXED_OPERATION_TIME = LocalDateTime.of(2026, 6, 4, 10, 3, 50);

    @Mock
    private OperationLogRepository operationLogRepository;

    private OperationLogService operationLogService;

    @BeforeEach
    void setUp() {
        operationLogService = new OperationLogService(operationLogRepository, FIXED_CLOCK);
    }

    @Test
    void recordLoginStoresOperatorSnapshotAndFormattedContent() {
        User operator = user(
                1L,
                "superadmin",
                "系统管理员",
                "13800000000",
                department("党群人力部"),
                List.of(
                        role("PARTY_HR_ADMIN", "人力管理员", true),
                        role("DISABLED", "停用角色", false),
                        role("SUPER_ADMIN", "超级管理员", true)));

        operationLogService.recordLogin(operator);

        OperationLog log = savedLog();
        assertThat(log.getOperatorUserId()).isEqualTo(1L);
        assertThat(log.getOperatorUsername()).isEqualTo("superadmin");
        assertThat(log.getOperatorRealName()).isEqualTo("系统管理员");
        assertThat(log.getOperatorDepartmentName()).isEqualTo("党群人力部");
        assertThat(log.getOperatorPhone()).isEqualTo("13800000000");
        assertThat(log.getOperatorRoleNames()).isEqualTo("人力管理员、超级管理员");
        assertThat(log.getOperationContent()).isEqualTo("superadmin 于 2026-06-04 10:03:50 进行登录");
        assertThat(log.getOperationTime()).isEqualTo(FIXED_OPERATION_TIME);
    }

    @Test
    void recordAppointmentCreatedStoresExpectedContent() {
        User operator = user(1L, "hradmin", "人事管理员", "13800000001", null, List.of());

        operationLogService.recordAppointmentCreated(operator, "张三");

        OperationLog log = savedLog();
        assertThat(log.getOperationContent()).isEqualTo("hradmin 于 2026-06-04 10:03:50 新增任免审批表：张三");
        assertThat(log.getOperatorDepartmentName()).isNull();
    }

    @Test
    void recordAppointmentUpdatedStoresExpectedContent() {
        User operator = user(1L, "hradmin", "人事管理员", "13800000001", null, List.of());

        operationLogService.recordAppointmentUpdated(operator, "李四");

        OperationLog log = savedLog();
        assertThat(log.getOperationContent()).isEqualTo("hradmin 于 2026-06-04 10:03:50 编辑任免审批表：李四");
    }

    @Test
    void recordRoleAssignedStoresTargetAccountAndSortedRoleNames() {
        User operator = user(1L, "superadmin", "系统管理员", "13800000000", null, List.of());
        User targetUser = user(2L, "targetUser", "目标用户", "13800000002", null, List.of());

        operationLogService.recordRoleAssigned(
                operator,
                targetUser,
                List.of(
                        role("GENERAL_ADMIN_MANAGER", "综合管理部管理员", true),
                        role("PARTY_HR_ADMIN", "党群人力部管理员", true)));

        OperationLog log = savedLog();
        assertThat(log.getOperationContent())
                .isEqualTo("superadmin 于 2026-06-04 10:03:50 为账号 targetUser 分配角色：党群人力部管理员、综合管理部管理员");
    }

    @Test
    void listAllMapsRepositoryResultsInReturnedOrder() {
        OperationLog first = operationLog(
                10L,
                "superadmin",
                "系统管理员",
                "党群人力部",
                "13800000000",
                "超级管理员",
                "first content");
        OperationLog second = operationLog(
                11L,
                "hradmin",
                "人事管理员",
                null,
                "13800000001",
                "人力管理员",
                "second content");
        when(operationLogRepository.findAllByOrderByOperationTimeDescIdDesc()).thenReturn(List.of(first, second));

        List<OperationLogResponse> responses = operationLogService.listAll();

        assertThat(responses)
                .extracting(OperationLogResponse::id)
                .containsExactly(10L, 11L);
        assertThat(responses.get(0))
                .usingRecursiveComparison()
                .isEqualTo(new OperationLogResponse(
                        10L,
                        "superadmin",
                        "系统管理员",
                        "党群人力部",
                        "13800000000",
                        "超级管理员",
                        "first content"));
        assertThat(responses.get(1).operationRecord()).isEqualTo("second content");
        verify(operationLogRepository).findAllByOrderByOperationTimeDescIdDesc();
        verify(operationLogRepository, never()).findAll();
    }

    @Test
    void listByDepartmentMapsRepositoryResultsForDepartmentName() {
        OperationLog log = operationLog(
                12L,
                "hradmin",
                "人事管理员",
                "党群人力部",
                "13800000001",
                "人力管理员",
                "department content");
        when(operationLogRepository.findByOperatorDepartmentNameOrderByOperationTimeDescIdDesc("党群人力部"))
                .thenReturn(List.of(log));

        List<OperationLogResponse> responses = operationLogService.listByDepartment("党群人力部");

        assertThat(responses)
                .singleElement()
                .usingRecursiveComparison()
                .isEqualTo(new OperationLogResponse(
                        12L,
                        "hradmin",
                        "人事管理员",
                        "党群人力部",
                        "13800000001",
                        "人力管理员",
                        "department content"));
        verify(operationLogRepository)
                .findByOperatorDepartmentNameOrderByOperationTimeDescIdDesc("党群人力部");
    }

    private OperationLog savedLog() {
        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private User user(
            Long id,
            String username,
            String realName,
            String phone,
            Department department,
            Collection<Role> roles) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setPasswordHash("encoded-password");
        user.setDepartment(department);
        user.setStatus(User.STATUS_ENABLED);
        user.setDeleted(false);
        user.getRoles().addAll(roles);
        return user;
    }

    private Department department(String name) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "name", name);
        return department;
    }

    private Role role(String code, String name, boolean enabled) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", code);
        ReflectionTestUtils.setField(role, "name", name);
        ReflectionTestUtils.setField(role, "enabled", enabled);
        return role;
    }

    private OperationLog operationLog(
            Long id,
            String operatorUsername,
            String operatorRealName,
            String operatorDepartmentName,
            String operatorPhone,
            String operatorRoleNames,
            String operationContent) {
        OperationLog log = new OperationLog();
        ReflectionTestUtils.setField(log, "id", id);
        log.setOperatorUsername(operatorUsername);
        log.setOperatorRealName(operatorRealName);
        log.setOperatorDepartmentName(operatorDepartmentName);
        log.setOperatorPhone(operatorPhone);
        log.setOperatorRoleNames(operatorRoleNames);
        log.setOperationContent(operationContent);
        log.setOperationTime(FIXED_OPERATION_TIME);
        return log;
    }
}
