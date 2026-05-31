package com.company.admin.file;

import com.company.admin.common.BusinessException;
import com.company.admin.file.dto.FileUploadResponse;
import com.company.admin.system.Department;
import com.company.admin.system.DepartmentAccessPolicy;
import com.company.admin.system.Permission;
import com.company.admin.system.Role;
import com.company.admin.system.User;
import com.company.admin.system.UserRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private static final long MAX_ID_PHOTO_SIZE_BYTES = 2L * 1024 * 1024;
    private static final double MIN_ID_PHOTO_RATIO = 0.65;
    private static final double MAX_ID_PHOTO_RATIO = 0.85;
    private static final String PARTY_HR_DEPARTMENT = "PARTY_HR";
    private static final String APPOINTMENT_MANAGE_PERMISSION = "appointment:manage";

    private final SysFileRepository sysFileRepository;
    private final UserRepository userRepository;
    private final Path uploadRoot;

    public FileService(
            SysFileRepository sysFileRepository,
            UserRepository userRepository,
            @Value("${app.upload.dir:${UPLOAD_DIR:uploads}}") String uploadDir) {
        this.sysFileRepository = sysFileRepository;
        this.userRepository = userRepository;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public FileUploadResponse uploadIdPhoto(String username, MultipartFile file) {
        User operator = requirePartyHrAppointmentManager(username);
        validateBasicFile(file);

        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = extension(originalName);
        validateMimeAndExtension(file.getContentType(), extension);

        BufferedImage image = readImage(file);
        validateIdPhotoRatio(image);

        String storedName = UUID.randomUUID() + extension;
        Path storagePath = storagePath(storedName);
        writeFile(file, storagePath);

        SysFile sysFile = new SysFile();
        sysFile.setOriginalName(originalName);
        sysFile.setStoredName(storedName);
        sysFile.setStoragePath(storagePath.toString());
        sysFile.setMimeType(file.getContentType());
        sysFile.setSizeBytes(file.getSize());
        sysFile.setBusinessType(FileBusinessTypes.ID_PHOTO);
        sysFile.setBusinessId(null);
        sysFile.setUploadedBy(operator.getId());
        sysFile.setUploadedAt(LocalDateTime.now());
        sysFile.setDeleted(false);

        // 落库供任免表引用：任免记录只保存 sys_file.id，避免业务表直接承载文件元数据。
        SysFile saved = sysFileRepository.save(sysFile);
        return new FileUploadResponse(
                saved.getId(),
                saved.getOriginalName(),
                "/api/files/" + saved.getId(),
                saved.getSizeBytes());
    }

    private User requirePartyHrAppointmentManager(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .filter(this::isEnabled)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录"));
        if (hasRole(user, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)) {
            return user;
        }

        Set<String> permissionCodes = permissionCodes(user);
        String departmentCode = departmentCode(user);
        if (!permissionCodes.contains(APPOINTMENT_MANAGE_PERMISSION) || !PARTY_HR_DEPARTMENT.equals(departmentCode)) {
            throw new AccessDeniedException("无权访问该资源");
        }
        return user;
    }

    private void validateBasicFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_ID_PHOTO_SIZE_BYTES) {
            throw new BusinessException("照片大小不能超过2MB");
        }
    }

    private String safeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        // 文件名去路径穿越：只保留客户端文件名末段，真实保存名另用 UUID 生成。
        String normalized = originalFilename.replace("\\", "/").replace("\0", "");
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (filename.isBlank() || ".".equals(filename) || "..".equals(filename)) {
            throw new BusinessException("文件名不能为空");
        }
        return filename;
    }

    private String extension(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            throw new BusinessException("仅支持 JPG/JPEG/PNG 图片");
        }
        return originalName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private void validateMimeAndExtension(String contentType, String extension) {
        boolean png = "image/png".equalsIgnoreCase(contentType) && ".png".equals(extension);
        boolean jpeg = "image/jpeg".equalsIgnoreCase(contentType)
                && (".jpg".equals(extension) || ".jpeg".equals(extension));
        if (!png && !jpeg) {
            throw new BusinessException("仅支持 JPG/JPEG/PNG 图片");
        }
    }

    private BufferedImage readImage(MultipartFile file) {
        try (var input = file.getInputStream()) {
            // 校验真实图片：不能只信任浏览器传来的 MIME 类型和扩展名。
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new BusinessException("上传文件不是有效图片");
            }
            return image;
        } catch (IOException exception) {
            throw new BusinessException("读取图片失败");
        }
    }

    private void validateIdPhotoRatio(BufferedImage image) {
        double ratio = image.getWidth() / (double) image.getHeight();
        // 限制证件照比例：一寸照片接近竖版头像，过方或过窄都拒绝。
        if (ratio < MIN_ID_PHOTO_RATIO || ratio > MAX_ID_PHOTO_RATIO) {
            throw new BusinessException("照片比例不符合一寸证件照要求");
        }
    }

    private Path storagePath(String storedName) {
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException("文件名不合法");
        }
        return target;
    }

    private void writeFile(MultipartFile file, Path storagePath) {
        try {
            Files.createDirectories(uploadRoot);
            Files.copy(file.getInputStream(), storagePath);
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "保存文件失败");
        }
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream()
                .filter(Role::isEnabled)
                .map(Role::getCode)
                .anyMatch(roleCode::equals);
    }

    private Set<String> permissionCodes(User user) {
        return user.getRoles().stream()
                .filter(Role::isEnabled)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String departmentCode(User user) {
        Department department = user.getDepartment();
        return department == null ? null : department.getCode();
    }

    private boolean isEnabled(User user) {
        return User.STATUS_ENABLED.equals(user.getStatus());
    }
}
