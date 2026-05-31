package com.company.admin.file;

import com.company.admin.common.ApiResponse;
import com.company.admin.file.dto.FileDownloadResource;
import com.company.admin.file.dto.FileUploadResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('appointment:manage')")
    public ResponseEntity<Resource> readIdPhoto(Authentication authentication, @PathVariable Long id) {
        FileDownloadResource file = fileService.readIdPhoto(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.sizeBytes())
                .body(new FileSystemResource(file.path()));
    }
}
