package com.company.admin.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.admin.system.DepartmentAccessPolicy;
import com.company.admin.system.Role;
import com.company.admin.system.User;
import com.company.admin.system.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @TempDir
    private Path uploadDir;

    @Mock
    private SysFileRepository sysFileRepository;

    @Mock
    private UserRepository userRepository;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(sysFileRepository, userRepository, uploadDir.toString());
    }

    @Test
    void uploadDeletesWrittenFileWhenDatabaseSaveFails() throws Exception {
        User superadmin = user(1L, "superadmin", role(DepartmentAccessPolicy.SUPER_ADMIN_ROLE));
        when(userRepository.findByUsernameAndDeletedFalse("superadmin")).thenReturn(Optional.of(superadmin));
        when(sysFileRepository.save(any(SysFile.class))).thenThrow(new DataIntegrityViolationException("boom"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> fileService.uploadIdPhoto(
                        "superadmin",
                        imageFile("photo.png", MediaType.IMAGE_PNG_VALUE, "png", 295, 413)));

        try (var files = Files.list(uploadDir)) {
            assertThat(files.toList()).isEmpty();
        }
    }

    private MockMultipartFile imageFile(
            String originalName,
            String contentType,
            String formatName,
            int width,
            int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, out);
        return new MockMultipartFile("file", originalName, contentType, out.toByteArray());
    }

    private User user(Long id, String username, Role... roles) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setPhone("00000000000");
        user.setPasswordHash("encoded-password");
        user.setStatus(User.STATUS_ENABLED);
        user.setDeleted(false);
        user.getRoles().addAll(List.of(roles));
        return user;
    }

    private Role role(String code) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", code);
        ReflectionTestUtils.setField(role, "name", code);
        ReflectionTestUtils.setField(role, "enabled", true);
        return role;
    }
}
