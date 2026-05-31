package com.company.admin.file.dto;

import java.nio.file.Path;

public record FileDownloadResource(
        Path path,
        String mimeType,
        long sizeBytes) {
}
