# Settings Roles Operation Log Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build expandable settings menus, three-level role assignment, real-name registration, operation logs, and sidebar collapse while keeping the app deployable with Spring Boot, MySQL, Vue 3, TypeScript, and Element Plus.

**Architecture:** Backend remains the source of truth for permissions, role assignment, and operation logs. Frontend keeps menu rendering thin by transforming the existing settings menu into a submenu and querying backend-filtered users, roles, and logs.

**Tech Stack:** Java 17, Spring Boot, Spring Security, Spring Data JPA, Flyway, MySQL/H2, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, Vitest.

---

## File Structure

- Modify `backend/src/main/resources/db/migration/V3__settings_roles_operation_logs.sql`: add `sys_user.real_name`, new roles, new permission, admin role permissions, and `sys_operation_log`.
- Modify `backend/src/main/java/com/company/admin/system/User.java`: add `realName`.
- Modify `backend/src/main/java/com/company/admin/auth/dto/RegisterRequest.java`: add validated `realName`.
- Modify `backend/src/main/java/com/company/admin/auth/dto/AuthResponse.java`: return `realName`.
- Modify `backend/src/main/java/com/company/admin/system/dto/UserResponse.java`: return `realName`.
- Create `backend/src/main/java/com/company/admin/system/OperationLog.java`: JPA entity for operation log snapshots.
- Create `backend/src/main/java/com/company/admin/system/OperationLogRepository.java`: log queries for all logs and department-scoped logs.
- Create `backend/src/main/java/com/company/admin/system/dto/OperationLogResponse.java`: API response shape for the log table.
- Create `backend/src/main/java/com/company/admin/system/OperationLogService.java`: central log writer and reader.
- Modify `backend/src/main/java/com/company/admin/system/DepartmentAccessPolicy.java`: define admin role codes and role assignment boundaries.
- Modify `backend/src/main/java/com/company/admin/auth/AuthService.java`: save real name and write login logs.
- Modify `backend/src/main/java/com/company/admin/system/SystemService.java`: filter visible users, roles, and logs; enforce role assignment permissions; write role assignment logs.
- Modify `backend/src/main/java/com/company/admin/system/SystemController.java`: accept current authentication on settings endpoints and expose `GET /api/system/logs`.
- Modify `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`: write create/update operation logs.
- Modify backend tests under `backend/src/test/java/com/company/admin/...`: update register payloads, add role/log assertions, and cover migration.
- Modify `frontend/src/api/auth.ts`: add `realName` to auth types and register payload.
- Modify `frontend/src/api/system.ts`: add `realName`, operation log types, target-role query parameter, and log API.
- Modify `frontend/src/views/auth/RegisterView.vue`: add real-name field and validation.
- Modify `frontend/src/views/settings/UserSettingsView.vue`: show real names and keep role assignment refresh flow.
- Modify `frontend/src/components/settings/RoleAssignDialog.vue`: fetch roles with `targetUserId`.
- Create `frontend/src/views/settings/SystemLogsView.vue`: operation log table.
- Modify `frontend/src/router/access.ts` and `frontend/src/router/index.ts`: allow department admins into settings routes and add `/settings/logs`.
- Modify `frontend/src/components/PermissionMenu.vue`: render settings as an `el-sub-menu`.
- Modify `frontend/src/layouts/AppLayout.vue` and `frontend/src/styles/main.css`: sidebar collapse button and responsive styles.
- Modify or add frontend tests under `frontend/src/...`: route guard, register form, role dialog, menu, and logs page.

---

### Task 1: Backend Migration And Domain Fields

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__settings_roles_operation_logs.sql`
- Modify: `backend/src/main/java/com/company/admin/system/User.java`
- Modify: `backend/src/main/java/com/company/admin/auth/dto/RegisterRequest.java`
- Modify: `backend/src/main/java/com/company/admin/auth/dto/AuthResponse.java`
- Modify: `backend/src/main/java/com/company/admin/system/dto/UserResponse.java`
- Test: `backend/src/test/java/com/company/admin/MigrationSmokeTest.java`

- [ ] **Step 1: Write the failing migration smoke assertion**

Add assertions to `MigrationSmokeTest` after Flyway migration setup to verify the new user field, roles, permission, and log table exist:

```java
Integer realNameColumnCount = isolatedJdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'real_name'
        """, Integer.class);
assertThat(realNameColumnCount).isEqualTo(1);

Integer operationLogTableCount = isolatedJdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_NAME = 'sys_operation_log'
        """, Integer.class);
assertThat(operationLogTableCount).isEqualTo(1);

Integer roleCount = isolatedJdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM sys_role
        WHERE code IN ('PARTY_HR_ADMIN', 'GENERAL_ADMIN_MANAGER')
        """, Integer.class);
assertThat(roleCount).isEqualTo(2);

Integer logPermissionCount = isolatedJdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM sys_permission
        WHERE code = 'system:operation-log'
        """, Integer.class);
assertThat(logPermissionCount).isEqualTo(1);
```

- [ ] **Step 2: Run the migration smoke test and verify it fails**

Run:

```powershell
.\mvnw -pl backend -Dtest=MigrationSmokeTest test
```

Expected: FAIL because `real_name`, `sys_operation_log`, new roles, and `system:operation-log` do not exist yet.

- [ ] **Step 3: Add the V3 migration**

Create `backend/src/main/resources/db/migration/V3__settings_roles_operation_logs.sql`:

```sql
ALTER TABLE sys_user
  ADD COLUMN real_name VARCHAR(64) NOT NULL DEFAULT '';

UPDATE sys_user
SET real_name = username
WHERE real_name = '';

INSERT INTO sys_role(code, name)
SELECT 'PARTY_HR_ADMIN', '党群人力部管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'PARTY_HR_ADMIN');

INSERT INTO sys_role(code, name)
SELECT 'GENERAL_ADMIN_MANAGER', '综合管理部管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'GENERAL_ADMIN_MANAGER');

INSERT INTO sys_permission(code, name, description)
SELECT 'system:operation-log', '系统日志', '查看系统操作记录'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:operation-log');

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('menu:settings', 'system:user-role', 'system:operation-log')
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('menu:party-hr', 'appointment:manage', 'menu:settings', 'system:user-role', 'system:operation-log')
WHERE r.code = 'PARTY_HR_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('menu:general-admin', 'menu:settings', 'system:user-role', 'system:operation-log')
WHERE r.code = 'GENERAL_ADMIN_MANAGER'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_user_id BIGINT NULL,
  operator_username VARCHAR(64) NOT NULL,
  operator_real_name VARCHAR(64) NOT NULL,
  operator_department_name VARCHAR(100) NULL,
  operator_phone VARCHAR(32) NOT NULL,
  operator_role_names VARCHAR(500) NOT NULL,
  operation_content VARCHAR(1000) NOT NULL,
  operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sys_operation_log_user FOREIGN KEY (operator_user_id) REFERENCES sys_user(id)
);

CREATE INDEX idx_sys_operation_log_time ON sys_operation_log(operation_time);
CREATE INDEX idx_sys_operation_log_department ON sys_operation_log(operator_department_name);
```

- [ ] **Step 4: Add realName to backend request/response types and entity**

Update `User.java`:

```java
@Column(name = "real_name", nullable = false, length = 64)
private String realName;

public String getRealName() {
    return realName;
}

public void setRealName(String realName) {
    this.realName = realName;
}
```

Update `RegisterRequest.java` constructor fields:

```java
@NotBlank(message = "不能为空")
@Size(max = 64, message = "长度不能超过64个字符")
String realName,
```

Update `AuthResponse.java`:

```java
Long userId,
String username,
String realName,
String phone,
String departmentCode,
String departmentName,
List<String> roles,
List<String> permissions,
String token
```

Update `UserResponse.java`:

```java
Long id,
String username,
String realName,
String phone,
String departmentCode,
String departmentName,
List<String> roles,
String status
```

- [ ] **Step 5: Run migration smoke test and compile check**

Run:

```powershell
.\mvnw -pl backend -Dtest=MigrationSmokeTest test
```

Expected: migration assertions PASS, compile may still fail in services/tests until later tasks update constructors.

- [ ] **Step 6: Commit backend migration and DTO field scaffolding**

```powershell
git add backend/src/main/resources/db/migration/V3__settings_roles_operation_logs.sql backend/src/main/java/com/company/admin/system/User.java backend/src/main/java/com/company/admin/auth/dto/RegisterRequest.java backend/src/main/java/com/company/admin/auth/dto/AuthResponse.java backend/src/main/java/com/company/admin/system/dto/UserResponse.java backend/src/test/java/com/company/admin/MigrationSmokeTest.java
git commit -m "feat: add user real name and operation log schema"
```

---

### Task 2: Backend Operation Log Model And Service

**Files:**
- Create: `backend/src/main/java/com/company/admin/system/OperationLog.java`
- Create: `backend/src/main/java/com/company/admin/system/OperationLogRepository.java`
- Create: `backend/src/main/java/com/company/admin/system/dto/OperationLogResponse.java`
- Create: `backend/src/main/java/com/company/admin/system/OperationLogService.java`
- Test: `backend/src/test/java/com/company/admin/system/OperationLogServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Create `OperationLogServiceTest.java` with tests for snapshot content and timestamp formatting:

```java
@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock
    private OperationLogRepository operationLogRepository;

    private final Clock clock = Clock.fixed(
            java.time.Instant.parse("2026-06-04T02:03:50Z"),
            java.time.ZoneId.of("Asia/Shanghai"));

    private OperationLogService operationLogService;

    @BeforeEach
    void setUp() {
        operationLogService = new OperationLogService(operationLogRepository, clock);
    }

    @Test
    void recordLoginStoresSnapshotAndFormattedContent() {
        User user = user("superadmin", "superadmin", null, role("SUPER_ADMIN", "超级管理员"));

        operationLogService.recordLogin(user);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        OperationLog log = captor.getValue();
        assertThat(log.getOperatorUsername()).isEqualTo("superadmin");
        assertThat(log.getOperatorRealName()).isEqualTo("superadmin");
        assertThat(log.getOperatorRoleNames()).isEqualTo("超级管理员");
        assertThat(log.getOperationContent()).isEqualTo("superadmin 于 2026-06-04 10:03:50 进行登录");
    }
}
```

- [ ] **Step 2: Run the new test and verify it fails**

Run:

```powershell
.\mvnw -pl backend -Dtest=OperationLogServiceTest test
```

Expected: FAIL because `OperationLogService` and entity do not exist.

- [ ] **Step 3: Add the operation log entity**

Create `OperationLog.java`:

```java
@Entity
@Table(name = "sys_operation_log")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "operator_username", nullable = false, length = 64)
    private String operatorUsername;

    @Column(name = "operator_real_name", nullable = false, length = 64)
    private String operatorRealName;

    @Column(name = "operator_department_name", length = 100)
    private String operatorDepartmentName;

    @Column(name = "operator_phone", nullable = false, length = 32)
    private String operatorPhone;

    @Column(name = "operator_role_names", nullable = false, length = 500)
    private String operatorRoleNames;

    @Column(name = "operation_content", nullable = false, length = 1000)
    private String operationContent;

    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    public Long getId() { return id; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }
    public String getOperatorRealName() { return operatorRealName; }
    public void setOperatorRealName(String operatorRealName) { this.operatorRealName = operatorRealName; }
    public String getOperatorDepartmentName() { return operatorDepartmentName; }
    public void setOperatorDepartmentName(String operatorDepartmentName) { this.operatorDepartmentName = operatorDepartmentName; }
    public String getOperatorPhone() { return operatorPhone; }
    public void setOperatorPhone(String operatorPhone) { this.operatorPhone = operatorPhone; }
    public String getOperatorRoleNames() { return operatorRoleNames; }
    public void setOperatorRoleNames(String operatorRoleNames) { this.operatorRoleNames = operatorRoleNames; }
    public String getOperationContent() { return operationContent; }
    public void setOperationContent(String operationContent) { this.operationContent = operationContent; }
    public LocalDateTime getOperationTime() { return operationTime; }
    public void setOperationTime(LocalDateTime operationTime) { this.operationTime = operationTime; }
}
```

- [ ] **Step 4: Add repository and response DTO**

Create `OperationLogRepository.java`:

```java
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findAllByOrderByOperationTimeDescIdDesc();

    List<OperationLog> findByOperatorDepartmentNameOrderByOperationTimeDescIdDesc(String departmentName);
}
```

Create `OperationLogResponse.java`:

```java
public record OperationLogResponse(
        Long id,
        String operatorUsername,
        String realName,
        String department,
        String phone,
        String role,
        String operationRecord) {
}
```

- [ ] **Step 5: Add the log service**

Create `OperationLogService.java` with Chinese comments on snapshot behavior:

```java
@Service
public class OperationLogService {
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogRepository operationLogRepository;
    private final Clock clock;

    public OperationLogService(OperationLogRepository operationLogRepository, Clock clock) {
        this.operationLogRepository = operationLogRepository;
        this.clock = clock;
    }

    public void recordLogin(User operator) {
        LocalDateTime operationTime = LocalDateTime.now(clock);
        record(operator, operationTime, operator.getUsername() + " 于 "
                + DISPLAY_TIME_FORMATTER.format(operationTime) + " 进行登录");
    }

    public void recordAppointmentCreated(User operator, String appointmentName) {
        LocalDateTime operationTime = LocalDateTime.now(clock);
        record(operator, operationTime, operator.getUsername() + " 于 "
                + DISPLAY_TIME_FORMATTER.format(operationTime) + " 新增任免审批表：" + appointmentName);
    }

    public void recordAppointmentUpdated(User operator, String appointmentName) {
        LocalDateTime operationTime = LocalDateTime.now(clock);
        record(operator, operationTime, operator.getUsername() + " 于 "
                + DISPLAY_TIME_FORMATTER.format(operationTime) + " 编辑任免审批表：" + appointmentName);
    }

    public void recordRoleAssigned(User operator, User targetUser, List<Role> roles) {
        LocalDateTime operationTime = LocalDateTime.now(clock);
        String roleNames = roles.stream()
                .map(Role::getName)
                .sorted()
                .collect(Collectors.joining("、"));
        record(operator, operationTime, operator.getUsername() + " 于 "
                + DISPLAY_TIME_FORMATTER.format(operationTime) + " 为账号 "
                + targetUser.getUsername() + " 分配角色：" + roleNames);
    }

    public List<OperationLogResponse> listAll() {
        return operationLogRepository.findAllByOrderByOperationTimeDescIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OperationLogResponse> listByDepartment(String departmentName) {
        return operationLogRepository.findByOperatorDepartmentNameOrderByOperationTimeDescIdDesc(departmentName).stream()
                .map(this::toResponse)
                .toList();
    }

    private void record(User operator, LocalDateTime operationTime, String operationContent) {
        OperationLog log = new OperationLog();
        log.setOperatorUserId(operator.getId());
        log.setOperatorUsername(operator.getUsername());
        log.setOperatorRealName(operator.getRealName());
        log.setOperatorDepartmentName(operator.getDepartment() == null ? null : operator.getDepartment().getName());
        log.setOperatorPhone(operator.getPhone());
        // 日志保存角色中文快照，后续角色改名不会影响历史审计记录。
        log.setOperatorRoleNames(operator.getRoles().stream()
                .filter(Role::isEnabled)
                .map(Role::getName)
                .sorted()
                .collect(Collectors.joining("、")));
        log.setOperationContent(operationContent);
        log.setOperationTime(operationTime);
        operationLogRepository.save(log);
    }

    private OperationLogResponse toResponse(OperationLog log) {
        return new OperationLogResponse(
                log.getId(),
                log.getOperatorUsername(),
                log.getOperatorRealName(),
                log.getOperatorDepartmentName(),
                log.getOperatorPhone(),
                log.getOperatorRoleNames(),
                log.getOperationContent());
    }
}
```

- [ ] **Step 6: Run service tests**

Run:

```powershell
.\mvnw -pl backend -Dtest=OperationLogServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit log model and service**

```powershell
git add backend/src/main/java/com/company/admin/system/OperationLog.java backend/src/main/java/com/company/admin/system/OperationLogRepository.java backend/src/main/java/com/company/admin/system/OperationLogService.java backend/src/main/java/com/company/admin/system/dto/OperationLogResponse.java backend/src/test/java/com/company/admin/system/OperationLogServiceTest.java
git commit -m "feat: add operation log service"
```

---

### Task 3: Backend Auth And Registration Real Name

**Files:**
- Modify: `backend/src/main/java/com/company/admin/auth/AuthService.java`
- Modify: `backend/src/test/java/com/company/admin/auth/AuthControllerTest.java`
- Modify: `backend/src/test/java/com/company/admin/auth/AuthServiceTest.java`
- Update helper payloads in backend tests that call `/api/auth/register`

- [ ] **Step 1: Write failing auth controller assertions**

Update register payloads to include `"realName", "张三"` and assert `$.data.realName`.

```java
.content(objectMapper.writeValueAsString(Map.of(
        "username", username,
        "realName", "张三",
        "password", "StrongPass123",
        "phone", "13800138000",
        "departmentCode", "PARTY_HR"))))
.andExpect(jsonPath("$.data.realName").value("张三"))
```

Add validation test:

```java
@Test
void registerWithoutRealNameReturnsValidationError() throws Exception {
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "username", uniqueUsername("no_name"),
                            "password", "StrongPass123",
                            "phone", "13500135000",
                            "departmentCode", "PARTY_HR"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("不能为空")));
}
```

- [ ] **Step 2: Run auth tests and verify they fail**

Run:

```powershell
.\mvnw -pl backend -Dtest=AuthControllerTest,AuthServiceTest test
```

Expected: FAIL because services still do not set `realName` and tests not fully updated.

- [ ] **Step 3: Update `AuthService.register` and response mapping**

Set real name during registration and include it in `AuthResponse`:

```java
user.setUsername(request.username().trim());
user.setRealName(request.realName().trim());
user.setPhone(request.phone().trim());
```

Update `toAuthResponse` constructor call:

```java
return new AuthResponse(
        user.getId(),
        user.getUsername(),
        user.getRealName(),
        user.getPhone(),
        department == null ? null : department.getCode(),
        department == null ? null : department.getName(),
        roleCodes,
        permissionCodes,
        jwtService.generateToken(user.getUsername()));
```

- [ ] **Step 4: Inject `OperationLogService` and log successful login**

Change constructor and field:

```java
private final OperationLogService operationLogService;
```

After `user.setLastLoginTime(LocalDateTime.now(clock));`:

```java
operationLogService.recordLogin(user);
```

- [ ] **Step 5: Update unit tests for new constructor and request signature**

In `AuthServiceTest`, mock `OperationLogService` and use:

```java
RegisterRequest request = new RegisterRequest(
        "race_user",
        "张三",
        "StrongPass123",
        "13600136000",
        "PARTY_HR");
```

- [ ] **Step 6: Run auth tests**

Run:

```powershell
.\mvnw -pl backend -Dtest=AuthControllerTest,AuthServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit auth real-name support**

```powershell
git add backend/src/main/java/com/company/admin/auth/AuthService.java backend/src/test/java/com/company/admin/auth/AuthControllerTest.java backend/src/test/java/com/company/admin/auth/AuthServiceTest.java
git commit -m "feat: add real name registration"
```

---

### Task 4: Backend Three-Level Role Permissions And Logs API

**Files:**
- Modify: `backend/src/main/java/com/company/admin/system/DepartmentAccessPolicy.java`
- Modify: `backend/src/main/java/com/company/admin/system/SystemService.java`
- Modify: `backend/src/main/java/com/company/admin/system/SystemController.java`
- Modify: `backend/src/main/java/com/company/admin/system/dto/RoleResponse.java`
- Modify: `backend/src/test/java/com/company/admin/system/SystemControllerTest.java`
- Modify: `backend/src/test/java/com/company/admin/system/SystemServiceTest.java`

- [ ] **Step 1: Write failing controller tests for department admins**

Add tests that promote a party HR user to admin with SQL, then assert scoped users, roles, and logs access:

```java
@Test
void partyHrAdminCanReadOnlyPartyHrUsersAndAssignableRoles() throws Exception {
    AuthPayload partyAdmin = registerUser("party_admin", "PARTY_HR");
    assignRoleInDatabase(partyAdmin.id(), "PARTY_HR_ADMIN");
    AuthPayload generalUser = registerUser("general_user", "GENERAL_ADMIN");

    mockMvc.perform(get("/api/system/users")
                    .header("Authorization", "Bearer " + partyAdmin.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].username", hasItem(partyAdmin.username())))
            .andExpect(jsonPath("$.data[*].username", not(hasItem(generalUser.username()))));

    mockMvc.perform(get("/api/system/roles")
                    .param("targetUserId", String.valueOf(partyAdmin.id()))
                    .header("Authorization", "Bearer " + partyAdmin.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].code", hasItem("PARTY_HR_ADMIN")))
            .andExpect(jsonPath("$.data[*].code", hasItem("PARTY_HR_USER")))
            .andExpect(jsonPath("$.data[*].code", not(hasItem("SUPER_ADMIN"))))
            .andExpect(jsonPath("$.data[*].code", not(hasItem("GENERAL_ADMIN_MANAGER"))));
}
```

Add helper:

```java
private void assignRoleInDatabase(Long userId, String roleCode) {
    jdbcTemplate.update("""
            INSERT INTO sys_user_role(user_id, role_id)
            SELECT ?, r.id
            FROM sys_role r
            WHERE r.code = ?
            """, userId, roleCode);
}
```

- [ ] **Step 2: Run system controller tests and verify failures**

Run:

```powershell
.\mvnw -pl backend -Dtest=SystemControllerTest,SystemServiceTest test
```

Expected: FAIL because endpoints still require `SUPER_ADMIN` and role filters are not implemented.

- [ ] **Step 3: Update policy constants and allowed assignments**

In `DepartmentAccessPolicy`, define:

```java
public static final String PARTY_HR_ADMIN_ROLE = "PARTY_HR_ADMIN";
public static final String GENERAL_ADMIN_MANAGER_ROLE = "GENERAL_ADMIN_MANAGER";
private static final String PARTY_HR_ROLE = "PARTY_HR_USER";
private static final String GENERAL_ADMIN_ROLE = "GENERAL_ADMIN_USER";
```

Allow department role pairs:

```java
private Set<String> allowedRoleCodesForDepartment(String departmentCode) {
    if ("PARTY_HR".equals(departmentCode)) {
        return new LinkedHashSet<>(List.of(DEFAULT_DEPARTMENT_ROLE, PARTY_HR_ADMIN_ROLE, PARTY_HR_ROLE));
    }
    if ("GENERAL_ADMIN".equals(departmentCode)) {
        return new LinkedHashSet<>(List.of(DEFAULT_DEPARTMENT_ROLE, GENERAL_ADMIN_MANAGER_ROLE, GENERAL_ADMIN_ROLE));
    }
    return Set.of(SUPER_ADMIN_ROLE);
}
```

- [ ] **Step 4: Refactor `SystemService` methods to receive operator username**

Change signatures:

```java
public List<UserResponse> users(String operatorUsername)
public List<RoleResponse> roles(String operatorUsername, Long targetUserId)
public UserResponse assignRoles(String operatorUsername, Long userId, AssignRolesRequest request)
public List<OperationLogResponse> operationLogs(String operatorUsername)
```

Implement helper:

```java
private User requireSettingsManager(String username) {
    User operator = userRepository.findByUsernameAndDeletedFalse(username)
            .filter(this::isEnabled)
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录"));
    if (!isSettingsManager(operator)) {
        throw new AccessDeniedException("无权访问该资源");
    }
    return operator;
}
```

- [ ] **Step 5: Implement scoped users and roles**

Use role checks:

```java
private boolean isSettingsManager(User user) {
    return hasRole(user, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)
            || hasRole(user, DepartmentAccessPolicy.PARTY_HR_ADMIN_ROLE)
            || hasRole(user, DepartmentAccessPolicy.GENERAL_ADMIN_MANAGER_ROLE);
}
```

For `users(operatorUsername)`:

```java
if (hasRole(operator, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)) {
    return userRepository.findByDeletedFalseOrderByIdAsc().stream().map(this::toUserResponse).toList();
}
String departmentCode = departmentCode(operator);
return userRepository.findByDeletedFalseOrderByIdAsc().stream()
        .filter(user -> departmentCode.equals(departmentCode(user)))
        .map(this::toUserResponse)
        .toList();
```

For `roles(operatorUsername, targetUserId)`, superadmin target `superadmin` returns only `SUPER_ADMIN`; otherwise return allowed non-legacy roles for the target department.

- [ ] **Step 6: Enforce scoped assignment and log role changes**

In `assignRoles`, verify operator can manage target user before changing roles. After saving:

```java
operationLogService.recordRoleAssigned(operator, user, roles);
return toUserResponse(user);
```

Keep last-superadmin lock before clearing roles.

- [ ] **Step 7: Update controller authorization**

Use authenticated endpoints and let service enforce scope:

```java
@GetMapping("/users")
public ApiResponse<List<UserResponse>> users(Authentication authentication) {
    return ApiResponse.ok(systemService.users(authentication.getName()));
}

@GetMapping("/roles")
public ApiResponse<List<RoleResponse>> roles(
        Authentication authentication,
        @RequestParam Long targetUserId) {
    return ApiResponse.ok(systemService.roles(authentication.getName(), targetUserId));
}

@GetMapping("/logs")
public ApiResponse<List<OperationLogResponse>> logs(Authentication authentication) {
    return ApiResponse.ok(systemService.operationLogs(authentication.getName()));
}
```

- [ ] **Step 8: Run system tests**

Run:

```powershell
.\mvnw -pl backend -Dtest=SystemControllerTest,SystemServiceTest test
```

Expected: PASS.

- [ ] **Step 9: Commit role hierarchy and logs API**

```powershell
git add backend/src/main/java/com/company/admin/system backend/src/test/java/com/company/admin/system
git commit -m "feat: add scoped role management and logs API"
```

---

### Task 5: Backend Appointment Operation Logging

**Files:**
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`
- Modify: `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`

- [ ] **Step 1: Write failing appointment log integration assertions**

In create/update tests, query `/api/system/logs` after the operation:

```java
mockMvc.perform(get("/api/system/logs")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].operationRecord", hasItem(containsString("新增任免审批表"))));
```

For update:

```java
.andExpect(jsonPath("$.data[*].operationRecord", hasItem(containsString("编辑任免审批表"))));
```

- [ ] **Step 2: Run appointment tests and verify failure**

Run:

```powershell
.\mvnw -pl backend -Dtest=AppointmentControllerTest test
```

Expected: FAIL because appointment logging is not wired.

- [ ] **Step 3: Inject `OperationLogService` into `AppointmentService`**

Add field and constructor parameter:

```java
private final OperationLogService operationLogService;
```

Call after save in create:

```java
operationLogService.recordAppointmentCreated(operator, savedRecord.getName());
```

Call after mapping in update:

```java
operationLogService.recordAppointmentUpdated(operator, record.getName());
```

- [ ] **Step 4: Run appointment tests**

Run:

```powershell
.\mvnw -pl backend -Dtest=AppointmentControllerTest test
```

Expected: PASS.

- [ ] **Step 5: Commit appointment operation logging**

```powershell
git add backend/src/main/java/com/company/admin/appointment/AppointmentService.java backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java
git commit -m "feat: log appointment mutations"
```

---

### Task 6: Frontend API Types, Routes, And Access Rules

**Files:**
- Modify: `frontend/src/api/auth.ts`
- Modify: `frontend/src/api/system.ts`
- Modify: `frontend/src/router/access.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/router/routerGuard.spec.ts`

- [ ] **Step 1: Write failing route guard tests**

Update `routerGuard.spec.ts`:

```ts
it('allows department admins to access settings pages', () => {
  expect(canAccessPath('/settings/users', ['PARTY_HR_ADMIN'], ['menu:settings'], 'PARTY_HR')).toBe(true)
  expect(canAccessPath('/settings/logs', ['GENERAL_ADMIN_MANAGER'], ['menu:settings'], 'GENERAL_ADMIN')).toBe(true)
})

it('blocks department users from settings pages', () => {
  expect(canAccessPath('/settings/users', ['PARTY_HR_USER'], ['menu:party-hr'], 'PARTY_HR')).toBe(false)
  expect(canAccessPath('/settings/logs', ['GENERAL_ADMIN_USER'], ['menu:general-admin'], 'GENERAL_ADMIN')).toBe(false)
})
```

- [ ] **Step 2: Run frontend route tests and verify failure**

Run:

```powershell
npm --prefix frontend run test:unit -- --run frontend/src/router/routerGuard.spec.ts
```

Expected: FAIL because department admins are blocked from settings.

- [ ] **Step 3: Update frontend API types**

In `auth.ts`:

```ts
export interface RegisterRequest extends LoginRequest {
  realName: string
  phone: string
  departmentCode: 'PARTY_HR' | 'GENERAL_ADMIN'
}

export interface AuthUser {
  userId: number
  username: string
  realName: string
  phone: string
  departmentCode: string | null
  departmentName: string | null
  roles: string[]
  permissions: string[]
  token: string
}
```

In `system.ts`:

```ts
export interface SystemUser {
  id: number
  username: string
  realName: string
  phone: string
  departmentCode: string | null
  departmentName: string | null
  roles: string[]
  status: string
}

export interface OperationLog {
  id: number
  operatorUsername: string
  realName: string
  department: string | null
  phone: string
  role: string
  operationRecord: string
}

export const fetchRoles = (targetUserId: number) => {
  return http.get<unknown, SystemRole[]>('/system/roles', { params: { targetUserId } })
}

export const fetchOperationLogs = () => {
  return http.get<unknown, OperationLog[]>('/system/logs')
}
```

- [ ] **Step 4: Update route access**

In `access.ts`:

```ts
const SETTINGS_ADMIN_ROLES = ['PARTY_HR_ADMIN', 'GENERAL_ADMIN_MANAGER']

if (targetPath.startsWith('/settings')) {
  return permissions.includes('menu:settings') && SETTINGS_ADMIN_ROLES.some((role) => roles.includes(role))
}
```

Keep `SUPER_ADMIN` early return.

- [ ] **Step 5: Add `/settings/logs` route**

In `index.ts`, import and route:

```ts
import SystemLogsView from '@/views/settings/SystemLogsView.vue'

{
  path: 'settings/logs',
  component: SystemLogsView,
  meta: { requiresAuth: true }
}
```

- [ ] **Step 6: Run route tests**

Run:

```powershell
npm --prefix frontend run test:unit -- --run frontend/src/router/routerGuard.spec.ts
```

Expected: PASS.

- [ ] **Step 7: Commit frontend API and route access**

```powershell
git add frontend/src/api/auth.ts frontend/src/api/system.ts frontend/src/router/access.ts frontend/src/router/index.ts frontend/src/router/routerGuard.spec.ts
git commit -m "feat: add settings log routes and access"
```

---

### Task 7: Frontend Register, Role Assignment, And Logs View

**Files:**
- Modify: `frontend/src/views/auth/RegisterView.vue`
- Modify: `frontend/src/views/settings/UserSettingsView.vue`
- Modify: `frontend/src/components/settings/RoleAssignDialog.vue`
- Create: `frontend/src/views/settings/SystemLogsView.vue`
- Add or modify related Vitest specs.

- [ ] **Step 1: Write failing UI tests**

Add tests that assert:

```ts
expect(wrapper.text()).toContain('姓名')
expect(wrapper.text()).toContain('操作记录')
expect(wrapper.text()).toContain('操作账号')
expect(fetchRolesMock).toHaveBeenCalledWith(1)
```

- [ ] **Step 2: Run affected frontend tests and verify failure**

Run:

```powershell
npm --prefix frontend run test:unit -- --run
```

Expected: FAIL until components are updated.

- [ ] **Step 3: Add register real-name field**

In `RegisterView.vue`, update form:

```ts
const form = reactive<RegisterRequest>({
  username: '',
  realName: '',
  password: '',
  phone: '',
  departmentCode: 'PARTY_HR'
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  departmentCode: [{ required: true, message: '请选择部门', trigger: 'change' }]
}
```

Add template item:

```vue
<el-form-item label="姓名" prop="realName">
  <el-input
    v-model="form.realName"
    :prefix-icon="User"
    placeholder="请输入姓名"
    size="large"
  />
</el-form-item>
```

- [ ] **Step 4: Show real name in role assignment page**

In `UserSettingsView.vue`, add a column after account:

```vue
<el-table-column label="姓名" min-width="130" prop="realName" show-overflow-tooltip />
```

- [ ] **Step 5: Fetch roles by target user**

In `RoleAssignDialog.vue`:

```ts
roles.value = props.user ? await fetchRoles(props.user.id) : []
```

Keep selected role initialization from `props.user.roles`.

- [ ] **Step 6: Create system logs page**

Create `SystemLogsView.vue`:

```vue
<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { ElButton, ElCard, ElEmpty, ElMessage, ElSkeleton, ElTable, ElTableColumn } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { fetchOperationLogs, type OperationLog } from '@/api/system'

const logs = ref<OperationLog[]>([])
const loading = ref(false)
const hasLogs = computed(() => logs.value.length > 0)

const loadLogs = async () => {
  loading.value = true
  try {
    logs.value = await fetchOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作记录加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadLogs()
})
</script>

<template>
  <section class="settings-page">
    <div class="page-heading">
      <div>
        <h1>操作记录</h1>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadLogs">刷新</el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <el-skeleton v-if="loading && !hasLogs" class="table-skeleton" animated :rows="6" />
      <el-table v-else-if="hasLogs" :data="logs" row-key="id">
        <el-table-column label="操作账号" min-width="150" prop="operatorUsername" show-overflow-tooltip />
        <el-table-column label="姓名" min-width="120" prop="realName" show-overflow-tooltip />
        <el-table-column label="部门" min-width="140" prop="department" show-overflow-tooltip />
        <el-table-column label="手机号" min-width="140" prop="phone" show-overflow-tooltip />
        <el-table-column label="角色" min-width="180" prop="role" show-overflow-tooltip />
        <el-table-column label="操作记录" min-width="360" prop="operationRecord" show-overflow-tooltip />
      </el-table>
      <el-empty v-else description="暂无操作记录" />
    </el-card>
  </section>
</template>
```

- [ ] **Step 7: Run frontend tests**

Run:

```powershell
npm --prefix frontend run test:unit -- --run
```

Expected: PASS.

- [ ] **Step 8: Commit frontend forms and settings pages**

```powershell
git add frontend/src/views/auth/RegisterView.vue frontend/src/views/settings/UserSettingsView.vue frontend/src/components/settings/RoleAssignDialog.vue frontend/src/views/settings/SystemLogsView.vue frontend/src/**/*.spec.ts
git commit -m "feat: add real name and operation log UI"
```

---

### Task 8: Frontend Settings Submenu And Sidebar Collapse

**Files:**
- Modify: `frontend/src/components/PermissionMenu.vue`
- Modify: `frontend/src/layouts/AppLayout.vue`
- Modify: `frontend/src/styles/main.css`
- Add or modify tests for menu rendering.

- [ ] **Step 1: Write failing menu render test**

Add a component test that mounts `PermissionMenu` with a settings menu and asserts:

```ts
expect(wrapper.text()).toContain('设置')
expect(wrapper.text()).toContain('角色分配')
expect(wrapper.text()).toContain('系统日志')
```

- [ ] **Step 2: Run menu test and verify failure**

Run:

```powershell
npm --prefix frontend run test:unit -- --run frontend/src/components/PermissionMenu.spec.ts
```

Expected: FAIL because `PermissionMenu` only renders flat `el-menu-item`.

- [ ] **Step 3: Add submenu support to `PermissionMenu.vue`**

Use `ElSubMenu` and accept collapsed state:

```ts
defineProps<{
  menus: SystemMenu[]
  collapsed?: boolean
}>()

const settingMenu = computed(() => props.menus.find((menu) => menu.path.startsWith('/settings')))
const normalMenus = computed(() => props.menus.filter((menu) => !menu.path.startsWith('/settings')))
```

Template:

```vue
<el-sub-menu v-if="settingMenu" index="/settings">
  <template #title>
    <el-icon><Setting /></el-icon>
    <span>设置</span>
  </template>
  <el-menu-item index="/settings/users">角色分配</el-menu-item>
  <el-menu-item index="/settings/logs">系统日志</el-menu-item>
</el-sub-menu>
```

- [ ] **Step 4: Add sidebar collapse state**

In `AppLayout.vue`:

```ts
import { ArrowLeft, ArrowRight, Refresh, SwitchButton, User } from '@element-plus/icons-vue'
const sidebarCollapsed = ref(false)
const sidebarWidth = computed(() => (sidebarCollapsed.value ? '72px' : '224px'))
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
```

Template:

```vue
<el-aside class="app-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }" :width="sidebarWidth">
  <button class="sidebar-toggle" type="button" @click="toggleSidebar">
    <el-icon><component :is="sidebarCollapsed ? ArrowRight : ArrowLeft" /></el-icon>
  </button>
  ...
  <permission-menu v-else :menus="menuStore.menus" :collapsed="sidebarCollapsed" />
</el-aside>
```

- [ ] **Step 5: Add CSS for collapse and submenu**

In `main.css`:

```css
.app-sidebar {
  position: relative;
  transition: width 0.2s ease;
}

.app-sidebar.is-collapsed {
  padding-inline: 10px;
}

.app-sidebar.is-collapsed .app-brand div {
  display: none;
}

.sidebar-toggle {
  position: absolute;
  top: 50%;
  right: -14px;
  z-index: 3;
  width: 28px;
  height: 48px;
  border: 1px solid #d6deea;
  border-radius: 8px;
  background: #ffffff;
  color: #1f2937;
  cursor: pointer;
}

.permission-menu .el-sub-menu__title,
.permission-menu .el-menu-item {
  height: 44px;
  margin-bottom: 6px;
  border-radius: 8px;
  color: #cbd5e1;
  line-height: 44px;
}

.permission-menu .el-sub-menu__title:hover,
.permission-menu .el-menu-item:hover,
.permission-menu .el-menu-item.is-active {
  background: #ffffff;
  color: #1d4ed8;
}
```

- [ ] **Step 6: Run frontend tests**

Run:

```powershell
npm --prefix frontend run test:unit -- --run
```

Expected: PASS.

- [ ] **Step 7: Commit menu and sidebar UI**

```powershell
git add frontend/src/components/PermissionMenu.vue frontend/src/layouts/AppLayout.vue frontend/src/styles/main.css frontend/src/components/PermissionMenu.spec.ts
git commit -m "feat: add settings submenu and sidebar collapse"
```

---

### Task 9: Full Verification And Local Runtime Update

**Files:**
- No planned source edits unless verification exposes defects.

- [ ] **Step 1: Run full backend tests**

Run:

```powershell
.\mvnw -pl backend test
```

Expected: all backend tests PASS.

- [ ] **Step 2: Run full frontend unit tests**

Run:

```powershell
npm --prefix frontend run test:unit -- --run
```

Expected: all frontend tests PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm --prefix frontend run build
```

Expected: build PASS with generated `frontend/dist`.

- [ ] **Step 4: Rebuild and restart local Docker backend**

Run:

```powershell
docker compose up -d --build
```

Expected: backend container starts and Flyway applies `V3__settings_roles_operation_logs.sql`.

- [ ] **Step 5: Start or refresh local frontend dev server**

If no Vite server is running, run:

```powershell
npm --prefix frontend run dev -- --host 0.0.0.0
```

Expected: terminal prints a local URL, commonly `http://localhost:5173/`.

- [ ] **Step 6: Browser verification**

Open the local URL and verify:

- Login page and register page display Chinese normally.
- Register page has required “姓名”.
- `superadmin / xjyadmin` login succeeds.
- Left menu shows “党群人力部、综合管理部、设置”; “设置” expands to “角色分配、系统日志”.
- Sidebar arrow collapses and expands the sidebar.
- Role assignment for `superadmin` only shows “超级管理员”.
- System logs page title is “操作记录” and table columns are “操作账号、姓名、部门、手机号、角色、操作记录”.
- Login, appointment create, appointment edit, and role assignment each append a new log row.

- [ ] **Step 7: Final commit if verification fixes were needed**

If any verification fixes changed files:

```powershell
git add <changed-files>
git commit -m "fix: stabilize settings roles log feature"
```

Expected: `git status -sb` is clean except the branch may be ahead of remote.
