package com.company.admin.auth.dto;

import java.util.List;

public record AuthResponse(
        Long userId,
        String username,
        String phone,
        String departmentCode,
        String departmentName,
        List<String> roles,
        List<String> permissions,
        String token) {
}
