package com.company.admin.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_user_id", nullable = false)
    private Long operatorUserId;

    @Column(name = "operator_username", nullable = false, length = 64)
    private String operatorUsername;

    @Column(name = "operator_real_name", nullable = false, length = 64)
    private String operatorRealName;

    @Column(name = "operator_department_name", length = 100)
    private String operatorDepartmentName;

    @Column(name = "operator_phone", nullable = false, length = 32)
    private String operatorPhone;

    @Column(name = "operator_role_names", nullable = false, length = 512)
    private String operatorRoleNames;

    @Column(name = "operation_content", nullable = false, columnDefinition = "TEXT")
    private String operationContent;

    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    public Long getId() {
        return id;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public String getOperatorRealName() {
        return operatorRealName;
    }

    public void setOperatorRealName(String operatorRealName) {
        this.operatorRealName = operatorRealName;
    }

    public String getOperatorDepartmentName() {
        return operatorDepartmentName;
    }

    public void setOperatorDepartmentName(String operatorDepartmentName) {
        this.operatorDepartmentName = operatorDepartmentName;
    }

    public String getOperatorPhone() {
        return operatorPhone;
    }

    public void setOperatorPhone(String operatorPhone) {
        this.operatorPhone = operatorPhone;
    }

    public String getOperatorRoleNames() {
        return operatorRoleNames;
    }

    public void setOperatorRoleNames(String operatorRoleNames) {
        this.operatorRoleNames = operatorRoleNames;
    }

    public String getOperationContent() {
        return operationContent;
    }

    public void setOperationContent(String operationContent) {
        this.operationContent = operationContent;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
