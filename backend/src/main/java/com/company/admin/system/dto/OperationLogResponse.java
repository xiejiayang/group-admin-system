package com.company.admin.system.dto;

public record OperationLogResponse(
        Long id,
        String operatorUsername,
        String realName,
        String department,
        String phone,
        String role,
        String operationRecord) {
}
