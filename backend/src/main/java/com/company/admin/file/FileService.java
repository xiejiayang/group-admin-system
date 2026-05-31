package com.company.admin.file;

import com.company.admin.common.BusinessException;
import com.company.admin.file.dto.FileDownloadResource;
import com.company.admin.file.dto.FileUploadResponse;
import com.company.admin.system.Department;
import com.company.admin.system.DepartmentAccessPolicy;
import com.company.admin.system.Permission;
import com.company.admin.system.Role;
import com.company.admin.system.User;
import com.company.admin.system.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private static final long MAX_ID_PHOTO_SIZE_BYTES = 2L * 1024 * 1024;
    private static final long MAX_ID_PHOTO_PIXELS = 12_000_000L;
    private static final int MAX_ID_PHOTO_WIDTH = 5_000;
    private static final int MAX_ID_PHOTO_HEIGHT = 5_000;
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
        DetectedImage image = readImageMetadata(file);
        validateMimeAndExtension(file.getContentType(), extension, image);
        validateIdPhotoDimensions(image);

        String storedName = UUID.randomUUID() + extension;
        Path storagePath = storagePath(storedName);
        writeFile(file, storagePath);
        registerRollbackCleanup(storagePath);

        SysFile sysFile = new SysFile();
        sysFile.setOriginalName(originalName);
        sysFile.setStoredName(storedName);
        sysFile.setStoragePath(storagePath.toString());
        sysFile.setMimeType(image.mimeType());
        sysFile.setSizeBytes(file.getSize());
        sysFile.setBusinessType(FileBusinessTypes.ID_PHOTO);
        sysFile.setBusinessId(null);
        sysFile.setUploadedBy(operator.getId());
        sysFile.setUploadedAt(LocalDateTime.now());
        sysFile.setDeleted(false);

        // 上传文件涉及磁盘和数据库双写，落库失败时立即清理已写入文件，事务回滚时也会通过同步器清理孤儿文件。
        SysFile saved;
        try {
            saved = sysFileRepository.save(sysFile);
        } catch (RuntimeException exception) {
            deleteFileIfExists(storagePath);
            throw exception;
        }

        return new FileUploadResponse(
                saved.getId(),
                saved.getOriginalName(),
                "/api/files/" + saved.getId(),
                saved.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public FileDownloadResource readIdPhoto(String username, Long id) {
        requirePartyHrAppointmentManager(username);
        SysFile sysFile = sysFileRepository.findByIdAndBusinessTypeAndDeletedFalse(id, FileBusinessTypes.ID_PHOTO)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "文件不存在或不可用"));
        Path storagePath = readableStoragePath(sysFile.getStoragePath());
        return new FileDownloadResource(storagePath, sysFile.getMimeType(), sysFile.getSizeBytes());
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

    private void validateMimeAndExtension(String contentType, String extension, DetectedImage image) {
        boolean png = "image/png".equalsIgnoreCase(contentType)
                && ".png".equals(extension)
                && "image/png".equals(image.mimeType());
        boolean jpeg = "image/jpeg".equalsIgnoreCase(contentType)
                && (".jpg".equals(extension) || ".jpeg".equals(extension))
                && "image/jpeg".equals(image.mimeType());
        if (!png && !jpeg) {
            throw new BusinessException("仅支持 JPG/JPEG/PNG 图片");
        }
    }

    private DetectedImage readImageMetadata(MultipartFile file) {
        try (var input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new BusinessException("上传文件不是有效图片");
            }

            // 先用 ImageReader 读取真实格式和宽高，避免完整解码超大压缩图造成内存压力。
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BusinessException("上传文件不是有效图片");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                return new DetectedImage(
                        mimeTypeForFormat(reader.getFormatName()),
                        reader.getWidth(0),
                        reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException("读取图片失败");
        }
    }

    private String mimeTypeForFormat(String formatName) {
        String normalized = formatName == null ? "" : formatName.toLowerCase(Locale.ROOT);
        if ("png".equals(normalized)) {
            return "image/png";
        }
        if ("jpg".equals(normalized) || "jpeg".equals(normalized)) {
            return "image/jpeg";
        }
        throw new BusinessException("仅支持 JPG/JPEG/PNG 图片");
    }

    private void validateIdPhotoDimensions(DetectedImage image) {
        if (image.width() <= 0 || image.height() <= 0) {
            throw new BusinessException("上传文件不是有效图片");
        }
        long pixels = (long) image.width() * image.height();
        if (image.width() > MAX_ID_PHOTO_WIDTH
                || image.height() > MAX_ID_PHOTO_HEIGHT
                || pixels > MAX_ID_PHOTO_PIXELS) {
            throw new BusinessException("照片尺寸超出限制");
        }

        double ratio = image.width() / (double) image.height();
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

    private void registerRollbackCleanup(Path storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteFileIfExists(storagePath);
                }
            }
        });
    }

    private Path readableStoragePath(String storagePath) {
        Path target;
        try {
            target = Path.of(storagePath).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在或不可用");
        }

        // 下载同样必须鉴权，并且校验真实路径仍位于上传根目录内，避免数据库路径被篡改后读取任意文件。
        if (!target.startsWith(uploadRoot) || !Files.isRegularFile(target)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在或不可用");
        }

        try {
            Path realRoot = uploadRoot.toRealPath();
            Path realTarget = target.toRealPath();
            if (!realTarget.startsWith(realRoot)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在或不可用");
            }
            return realTarget;
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在或不可用");
        }
    }

    private void deleteFileIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
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

    private record DetectedImage(String mimeType, int width, int height) {
    }
}
