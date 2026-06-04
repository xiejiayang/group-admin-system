package com.company.admin.system;

import com.company.admin.system.dto.OperationLogResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationLogService {

    private static final ZoneId OPERATION_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter OPERATION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogRepository operationLogRepository;
    private final Clock clock;

    @Autowired
    public OperationLogService(OperationLogRepository operationLogRepository) {
        this(operationLogRepository, Clock.system(OPERATION_ZONE));
    }

    OperationLogService(OperationLogRepository operationLogRepository, Clock clock) {
        this.operationLogRepository = operationLogRepository;
        this.clock = clock;
    }

    @Transactional
    public void recordLogin(User operator) {
        LocalDateTime operationTime = currentOperationTime();
        saveOperation(operator, operationTime, operator.getUsername()
                + " 于 "
                + formatOperationTime(operationTime)
                + " 进行登录");
    }

    @Transactional
    public void recordAppointmentCreated(User operator, String personName) {
        LocalDateTime operationTime = currentOperationTime();
        saveOperation(operator, operationTime, operator.getUsername()
                + " 于 "
                + formatOperationTime(operationTime)
                + " 新增任免审批表："
                + personName);
    }

    @Transactional
    public void recordAppointmentEdited(User operator, String personName) {
        LocalDateTime operationTime = currentOperationTime();
        saveOperation(operator, operationTime, operator.getUsername()
                + " 于 "
                + formatOperationTime(operationTime)
                + " 编辑任免审批表："
                + personName);
    }

    @Transactional
    public void recordRoleAssignment(User operator, User targetUser, Collection<Role> assignedRoles) {
        LocalDateTime operationTime = currentOperationTime();
        saveOperation(operator, operationTime, operator.getUsername()
                + " 于 "
                + formatOperationTime(operationTime)
                + " 为账号 "
                + targetUser.getUsername()
                + " 分配角色："
                + roleNames(assignedRoles));
    }

    @Transactional(readOnly = true)
    public List<OperationLogResponse> listAll() {
        return operationLogRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperationLogResponse> listByDepartment(String departmentName) {
        return operationLogRepository.findByOperatorDepartmentName(departmentName).stream()
                .map(this::toResponse)
                .toList();
    }

    private void saveOperation(User operator, LocalDateTime operationTime, String operationContent) {
        OperationLog operationLog = new OperationLog();
        operationLog.setOperatorUserId(operator.getId());
        operationLog.setOperatorUsername(operator.getUsername());
        operationLog.setOperatorRealName(operator.getRealName());
        operationLog.setOperatorDepartmentName(departmentName(operator));
        operationLog.setOperatorPhone(operator.getPhone());
        // 操作日志保存操作人当时的资料快照，避免后续用户、部门或角色调整影响历史审计记录。
        operationLog.setOperatorRoleNames(roleNames(operator.getRoles()));
        operationLog.setOperationContent(operationContent);
        operationLog.setOperationTime(operationTime);
        operationLogRepository.save(operationLog);
    }

    private String roleNames(Collection<Role> roles) {
        // 角色中文名用于日志展示和审计留痕，只记录启用角色，并按名称排序后用顿号连接。
        return roles.stream()
                .filter(Role::isEnabled)
                .map(Role::getName)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("、"));
    }

    private String departmentName(User operator) {
        Department department = operator.getDepartment();
        return department == null ? null : department.getName();
    }

    private LocalDateTime currentOperationTime() {
        return LocalDateTime.now(clock);
    }

    private String formatOperationTime(LocalDateTime operationTime) {
        return operationTime.format(OPERATION_TIME_FORMATTER);
    }

    private OperationLogResponse toResponse(OperationLog operationLog) {
        return new OperationLogResponse(
                operationLog.getId(),
                operationLog.getOperatorUsername(),
                operationLog.getOperatorRealName(),
                operationLog.getOperatorDepartmentName(),
                operationLog.getOperatorPhone(),
                operationLog.getOperatorRoleNames(),
                operationLog.getOperationContent());
    }
}
