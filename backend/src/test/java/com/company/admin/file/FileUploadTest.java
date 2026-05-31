package com.company.admin.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = {
        "app.upload.dir=${java.io.tmpdir}/group-admin-system-file-upload-test",
        "spring.servlet.multipart.max-file-size=3MB",
        "spring.servlet.multipart.max-request-size=3MB"
})
@ActiveProfiles("test")
class FileUploadTest {

    private static final Path UPLOAD_DIR = Path.of(
            System.getProperty("java.io.tmpdir"),
            "group-admin-system-file-upload-test");

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMockMvc() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        jdbcTemplate.update("DELETE FROM appointment_family_member");
        jdbcTemplate.update("DELETE FROM appointment_record");
        jdbcTemplate.update("DELETE FROM sys_file");
        cleanUploadDir();
    }

    @Test
    void rejectsPlainTextFile() throws Exception {
        String token = loginSuperadmin().token();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/files/id-photo")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsFileLargerThanTwoMb() throws Exception {
        String token = loginSuperadmin().token();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[2 * 1024 * 1024 + 1]);

        mockMvc.perform(multipart("/api/files/id-photo")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsFakeImageContentEvenWithImageMimeType() throws Exception {
        String token = loginSuperadmin().token();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                MediaType.IMAGE_PNG_VALUE,
                "not really an image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/files/id-photo")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsImageWithInvalidIdPhotoRatio() throws Exception {
        String token = loginSuperadmin().token();
        MockMultipartFile file = imageFile("square.png", MediaType.IMAGE_PNG_VALUE, "png", 100, 100);

        mockMvc.perform(multipart("/api/files/id-photo")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void uploadsValidPngAndJpegAndPersistsSysFile() throws Exception {
        AuthPayload superadmin = loginSuperadmin();
        assertSuccessfulUpload(
                superadmin.token(),
                superadmin.id(),
                imageFile("photo.png", MediaType.IMAGE_PNG_VALUE, "png", 295, 413));

        AuthPayload partyHr = registerUser("task6_party_upload", "PARTY_HR");
        assertSuccessfulUpload(
                partyHr.token(),
                partyHr.id(),
                imageFile("headshot.jpeg", MediaType.IMAGE_JPEG_VALUE, "jpg", 295, 413));
    }

    @Test
    void generalAdminCannotUploadIdPhoto() throws Exception {
        AuthPayload generalAdmin = registerUser("task6_general_upload", "GENERAL_ADMIN");
        MockMultipartFile file = imageFile("photo.png", MediaType.IMAGE_PNG_VALUE, "png", 295, 413);

        mockMvc.perform(multipart("/api/files/id-photo")
                        .file(file)
                        .header("Authorization", "Bearer " + generalAdmin.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void assertSuccessfulUpload(String token, Long expectedUploadedBy, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/files/id-photo")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.originalName").value(file.getOriginalFilename()))
                .andExpect(jsonPath("$.data.sizeBytes").value(file.getSize()))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        Long fileId = data.path("id").asLong();
        assertThat(data.path("url").asText()).isEqualTo("/api/files/" + fileId);

        Map<String, Object> stored = jdbcTemplate.queryForMap("""
                SELECT original_name, stored_name, storage_path, mime_type, size_bytes,
                       business_type, business_id, uploaded_by, deleted
                FROM sys_file
                WHERE id = ?
                """, fileId);
        assertThat(stored.get("original_name")).isEqualTo(file.getOriginalFilename());
        String storedName = stored.get("stored_name").toString();
        String extension = extension(file.getOriginalFilename());
        assertThat(storedName).endsWith(extension).doesNotContain("..").doesNotContain("/");
        UUID.fromString(storedName.substring(0, storedName.length() - extension.length()));
        assertThat(stored.get("storage_path").toString()).doesNotContain(file.getOriginalFilename());
        assertThat(stored.get("mime_type")).isEqualTo(file.getContentType());
        assertThat(((Number) stored.get("size_bytes")).longValue()).isEqualTo(file.getSize());
        assertThat(stored.get("business_type")).isEqualTo(FileBusinessTypes.ID_PHOTO);
        assertThat(stored.get("business_id")).isNull();
        assertThat(((Number) stored.get("uploaded_by")).longValue()).isEqualTo(expectedUploadedBy);
        assertThat(stored.get("deleted")).isEqualTo(false);
        assertThat(Files.exists(Path.of(stored.get("storage_path").toString()))).isTrue();
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

    private String extension(String filename) {
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private void cleanUploadDir() throws Exception {
        Files.createDirectories(UPLOAD_DIR);
        try (var stream = Files.walk(UPLOAD_DIR)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(UPLOAD_DIR)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private AuthPayload loginSuperadmin() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "superadmin",
                                "password", "xjyadmin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return payload(response);
    }

    private AuthPayload registerUser(String prefix, String departmentCode) throws Exception {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "StrongPass123",
                                "phone", "13800138000",
                                "departmentCode", departmentCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return payload(response);
    }

    private AuthPayload payload(String response) throws Exception {
        JsonNode data = objectMapper.readTree(response).path("data");
        return new AuthPayload(
                data.path("userId").asLong(),
                data.path("username").asText(),
                data.path("token").asText());
    }

    private record AuthPayload(Long id, String username, String token) {
    }
}
