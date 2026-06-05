package com.company.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

    @Test
    void appointmentRecordHasPartyHrBoardColumns() throws SQLException {
        assertThat(columns("appointment_record"))
                .contains(
                        "global_sequence",
                        "display_sequence",
                        "company_name",
                        "department_name",
                        "political_status",
                        "marital_status",
                        "remark",
                        "full_time_education_degree",
                        "full_time_school",
                        "full_time_major",
                        "part_time_education",
                        "part_time_degree",
                        "part_time_school",
                        "part_time_major");
    }

    @Test
    void settingsRolesAndOperationLogMigrationCreatesRequiredSchema() {
        Integer realNameColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'sys_user'
                  AND LOWER(COLUMN_NAME) = 'real_name'
                """, Integer.class);
        Integer operationLogTableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE LOWER(TABLE_NAME) = 'sys_operation_log'
                """, Integer.class);
        Integer adminRoleCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role
                WHERE code IN ('PARTY_HR_ADMIN', 'GENERAL_ADMIN_ADMIN')
                """, Integer.class);
        Integer operationLogPermissionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_permission
                WHERE code = 'system:operation-log'
                """, Integer.class);
        String operationLogDepartmentNullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'sys_operation_log'
                  AND LOWER(COLUMN_NAME) = 'operator_department_name'
                """, String.class);

        assertThat(realNameColumnCount).isOne();
        assertThat(operationLogTableCount).isOne();
        assertThat(adminRoleCount).isEqualTo(2);
        assertThat(operationLogPermissionCount).isOne();
        assertThat(operationLogDepartmentNullable).isEqualTo("YES");
    }

    @Test
    void departmentRoleCleanupMigrationRenamesManagerAndRemovesDepartmentUser() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:department_role_cleanup_%d;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                        .formatted(System.nanoTime())
                        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        dataSource.setDriverClassName("org.h2.Driver");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("4")
                .load()
                .migrate();

        JdbcTemplate isolatedJdbcTemplate = new JdbcTemplate(dataSource);
        Long managerRoleId = isolatedJdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE code = 'GENERAL_ADMIN_MANAGER'",
                Long.class);
        Integer managerPermissionCount = isolatedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_permission WHERE role_id = ?",
                Integer.class,
                managerRoleId);
        isolatedJdbcTemplate.update("""
                INSERT INTO sys_user_role(user_id, role_id)
                SELECT u.id, r.id
                FROM sys_user u
                JOIN sys_role r ON r.code IN ('GENERAL_ADMIN_MANAGER', 'DEPARTMENT_USER')
                WHERE u.username = 'superadmin'
                """);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(isolatedJdbcTemplate.queryForList(
                        "SELECT code FROM sys_role ORDER BY code",
                        String.class))
                .containsExactly(
                        "GENERAL_ADMIN_ADMIN",
                        "GENERAL_ADMIN_USER",
                        "PARTY_HR_ADMIN",
                        "PARTY_HR_USER",
                        "SUPER_ADMIN");
        assertThat(isolatedJdbcTemplate.queryForObject(
                        "SELECT id FROM sys_role WHERE code = 'GENERAL_ADMIN_ADMIN'",
                        Long.class))
                .isEqualTo(managerRoleId);
        assertThat(isolatedJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_role_permission WHERE role_id = ?",
                        Integer.class,
                        managerRoleId))
                .isEqualTo(managerPermissionCount);
        assertThat(isolatedJdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM sys_user_role ur
                        JOIN sys_user u ON u.id = ur.user_id
                        JOIN sys_role r ON r.id = ur.role_id
                        WHERE u.username = 'superadmin' AND r.code = 'GENERAL_ADMIN_ADMIN'
                        """, Integer.class))
                .isOne();
        assertThat(isolatedJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_role WHERE code IN ('GENERAL_ADMIN_MANAGER', 'DEPARTMENT_USER')",
                        Integer.class))
                .isZero();
    }

    @Test
    void appointmentBoardMigrationBackfillsExistingRecordsAndCreatesIndexes() throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:appointment_board_migration_%d;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                        .formatted(System.nanoTime())
                        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        dataSource.setDriverClassName("org.h2.Driver");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();

        JdbcTemplate isolatedJdbcTemplate = new JdbcTemplate(dataSource);
        isolatedJdbcTemplate.update("""
                INSERT INTO appointment_record(name, phone, id_card, position_name, graduation_school, address)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "张三",
                "13800000000",
                "110101199001010000",
                "党委办公室主任",
                "中央党校",
                "北京市");
        isolatedJdbcTemplate.update("""
                INSERT INTO appointment_record(name, phone, id_card, position_name, graduation_school, address)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "same-company-second",
                "13800000001",
                "110101199001010001",
                "same-company-position",
                "same-company-school",
                "same-company-address");
        Long recordId = isolatedJdbcTemplate.queryForObject(
                "SELECT id FROM appointment_record WHERE phone = ?",
                Long.class,
                "13800000000");
        Long sameCompanyRecordId = isolatedJdbcTemplate.queryForObject(
                "SELECT id FROM appointment_record WHERE phone = ?",
                Long.class,
                "13800000001");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Map<String, Object> appointmentRecord = isolatedJdbcTemplate.queryForMap("""
                SELECT company_name, department_name, current_position, full_time_school,
                       global_sequence, display_sequence
                FROM appointment_record
                WHERE id = ?
                """, recordId);

        assertThat(appointmentRecord)
                .containsEntry("company_name", "集团公司")
                .containsEntry("department_name", "党群人力部")
                .containsEntry("current_position", "党委办公室主任")
                .containsEntry("full_time_school", "中央党校");
        assertThat(((Number) appointmentRecord.get("global_sequence")).longValue()).isEqualTo(1);
        assertThat(((Number) appointmentRecord.get("display_sequence")).longValue()).isEqualTo(recordId);
        Long sameCompanyGlobalSequence = isolatedJdbcTemplate.queryForObject(
                "SELECT global_sequence FROM appointment_record WHERE id = ?",
                Long.class,
                sameCompanyRecordId);
        assertThat(sameCompanyGlobalSequence).isEqualTo(1);
        assertThat(columnTypes(dataSource, "appointment_record"))
                .containsEntry("global_sequence", Types.BIGINT)
                .containsEntry("display_sequence", Types.BIGINT);
        assertThat(indexColumns(dataSource, "appointment_record"))
                .containsEntry(
                        "idx_appointment_record_company_sequence",
                        List.of("company_name", "global_sequence"))
                .containsEntry(
                        "idx_appointment_record_display_sequence",
                        List.of("display_sequence"));
    }

    @Test
    void appointmentBoardColumnsUseDefaultsForNewRecordsWrittenWithoutBoardFields() {
        DriverManagerDataSource dataSource = appointmentBoardMigrationDataSource();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate isolatedJdbcTemplate = new JdbcTemplate(dataSource);
        isolatedJdbcTemplate.update("""
                INSERT INTO appointment_record(name, phone, id_card, position_name, graduation_school, address)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "李四",
                "13900000000",
                "110101199002020000",
                "组织人事主管",
                "北京大学",
                "上海市");

        Map<String, Object> appointmentRecord = isolatedJdbcTemplate.queryForMap("""
                SELECT company_name, department_name, global_sequence, display_sequence
                FROM appointment_record
                WHERE phone = ?
                """, "13900000000");

        assertThat(appointmentRecord)
                .containsEntry("company_name", "集团公司")
                .containsEntry("department_name", "党群人力部");
        assertThat(((Number) appointmentRecord.get("global_sequence")).longValue()).isZero();
        assertThat(((Number) appointmentRecord.get("display_sequence")).longValue()).isZero();
    }

    private List<String> columns(String tableName) throws SQLException {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<String> columns = new ArrayList<>();
            try (ResultSet resultSet = metaData.getColumns(null, null, tableName, null)) {
                while (resultSet.next()) {
                    columns.add(resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            return columns;
        }
    }

    private Map<String, Integer> columnTypes(DriverManagerDataSource dataSource, String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(null, null, tableName, null)) {
                Map<String, Integer> columnTypes = new java.util.HashMap<>();
                while (resultSet.next()) {
                    columnTypes.put(
                            resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT),
                            resultSet.getInt("DATA_TYPE"));
                }
                return columnTypes;
            }
        }
    }

    private Map<String, List<String>> indexColumns(DriverManagerDataSource dataSource, String tableName)
            throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, Map<Short, String>> indexedColumns = new TreeMap<>();
            try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
                while (resultSet.next()) {
                    String indexName = resultSet.getString("INDEX_NAME");
                    String columnName = resultSet.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) {
                        indexedColumns
                                .computeIfAbsent(indexName.toLowerCase(Locale.ROOT), ignored -> new TreeMap<>())
                                .put(
                                        resultSet.getShort("ORDINAL_POSITION"),
                                        columnName.toLowerCase(Locale.ROOT));
                    }
                }
            }
            Map<String, List<String>> indexes = new TreeMap<>();
            indexedColumns.forEach((indexName, columns) -> indexes.put(indexName, List.copyOf(columns.values())));
            return indexes;
        }
    }

    private DriverManagerDataSource appointmentBoardMigrationDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:appointment_board_migration_%d;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                        .formatted(System.nanoTime())
                        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        dataSource.setDriverClassName("org.h2.Driver");
        return dataSource;
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
