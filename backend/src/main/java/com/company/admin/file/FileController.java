package com.company.admin.file;

import com.company.admin.common.ApiResponse;
import com.company.admin.file.dto.FileUploadResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/id-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('appointment:manage')")
    public ApiResponse<FileUploadResponse> uploadIdPhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(fileService.uploadIdPhoto(authentication.getName(), file));
    }
}
