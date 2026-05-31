package com.company.admin.file.dto;

public record FileUploadResponse(
        Long id,
        String originalName,
        String url,
        long sizeBytes) {
}
