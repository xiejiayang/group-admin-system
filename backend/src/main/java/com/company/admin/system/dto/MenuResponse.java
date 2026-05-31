package com.company.admin.system.dto;

public record MenuResponse(
        Long id,
        String name,
        String path,
        String permissionCode,
        Integer sortOrder) {
}
