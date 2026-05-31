package com.company.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MigrationSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsWithMigrations() {
    }

    @Test
    void productionJwtSecretRequiresEnvironmentVariableAndTestProfileHasLocalSecret() throws Exception {
        String applicationYaml = new String(
                new ClassPathResource("application.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        String[] documents = applicationYaml.split("(?m)^---\\s*$");

        assertThat(documents[0])
                .containsPattern("(?m)^\\s*secret:\\s*\\$\\{JWT_SECRET}\\s*$")
                .doesNotContain("${JWT_SECRET:");
        assertThat(documents).hasSizeGreaterThanOrEqualTo(2);
        assertThat(documents[1])
                .contains("on-profile: test")
                .contains("secret: group-admin-system-test-jwt-secret-2026-32-bytes");
    }

    @Test
    void createsDefaultSuperadminWithBcryptPasswordAndRole() {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM sys_user WHERE username = ?",
                String.class,
                "superadmin");

        assertThat(passwordHash)
                .isNotBlank()
                .isNotEqualTo("xjyadmin")
                .startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("xjyadmin", passwordHash)).isTrue();

        Integer roleBindingCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_user u ON u.id = ur.user_id
                JOIN sys_role r ON r.id = ur.role_id
                WHERE u.username = ? AND r.code = ?
                """, Integer.class, "superadmin", "SUPER_ADMIN");

        assertThat(roleBindingCount).isEqualTo(1);
    }

    @Test
    void createsForeignKeysForMenuPermissionAndAppointmentPhoto() throws SQLException {
        assertThat(importedKeys("sys_menu"))
                .anySatisfy(foreignKey -> {
                    assertThat(foreignKey.fkColumn()).isEqualToIgnoringCase("permission_code");
                    assertThat(foreignKey.pkTable()).isEqualToIgnoringCase("sys_permission");
                    assertThat(foreignKey.pkColumn()).isEqualToIgnoringCase("code");
                });

        assertThat(importedKeys("appointment_record"))
                .anySatisfy(foreignKey -> {
                    assertThat(foreignKey.fkColumn()).isEqualToIgnoringCase("photo_file_id");
                    assertThat(foreignKey.pkTable()).isEqualToIgnoringCase("sys_file");
                    assertThat(foreignKey.pkColumn()).isEqualToIgnoringCase("id");
                });
    }

    private List<ForeignKeyRef> importedKeys(String tableName) throws SQLException {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<ForeignKeyRef> keys = new ArrayList<>();
            try (ResultSet resultSet = metaData.getImportedKeys(null, null, tableName)) {
                while (resultSet.next()) {
                    keys.add(new ForeignKeyRef(
                            resultSet.getString("FKCOLUMN_NAME"),
                            resultSet.getString("PKTABLE_NAME"),
                            resultSet.getString("PKCOLUMN_NAME")));
                }
            }
            return keys;
        }
    }

    private record ForeignKeyRef(String fkColumn, String pkTable, String pkColumn) {
    }
}
