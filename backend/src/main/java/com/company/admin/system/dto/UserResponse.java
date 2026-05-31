package com.company.admin.system.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String phone,
        String departmentCode,
        String departmentName,
        List<String> roles,
        String status) {
}
