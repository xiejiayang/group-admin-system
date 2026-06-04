package com.company.admin.auth.dto;

import java.util.List;

public record AuthResponse(
        Long userId,
        String username,
        String realName,
        String phone,
        String departmentCode,
        String departmentName,
        List<String> roles,
        List<String> permissions,
        String token) {

    public AuthResponse(
            Long userId,
            String username,
            String phone,
            String departmentCode,
            String departmentName,
            List<String> roles,
            List<String> permissions,
            String token) {
        this(userId, username, username, phone, departmentCode, departmentName, roles, permissions, token);
    }
}
