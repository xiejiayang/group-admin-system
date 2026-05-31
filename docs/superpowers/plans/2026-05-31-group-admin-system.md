# Group Admin System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a deployable集团后台管理系统 with Spring Boot + MySQL backend and Vue 3 + TypeScript + Element Plus frontend.

**Architecture:** Use a modular monolith backend split into `auth`, `system`, `organization`, `appointment`, `file`, and `common` packages. Use a separate Vue frontend with route guards and backend-driven menus. Deploy with Docker Compose using Nginx as the public entrypoint and `/api` reverse proxy.

**Tech Stack:** Java 17+, Spring Boot, Maven, MySQL 8, Flyway, Spring Security JWT, Vue 3, TypeScript, Vite, Element Plus, Pinia, Vue Router, Axios, Docker Compose, Nginx.

---

## File Structure

Create this top-level structure:

- `backend/`: Spring Boot API service.
- `backend/src/main/java/com/company/admin/common/`: response wrapper, exceptions, audit base classes, security helpers.
- `backend/src/main/java/com/company/admin/auth/`: registration, login, current user, JWT.
- `backend/src/main/java/com/company/admin/system/`: users, roles, permissions, menus.
- `backend/src/main/java/com/company/admin/organization/`: departments.
- `backend/src/main/java/com/company/admin/appointment/`: party HR appointment records and family members.
- `backend/src/main/java/com/company/admin/file/`: id photo upload and file access.
- `backend/src/main/resources/db/migration/`: Flyway schema and seed SQL.
- `backend/src/test/java/com/company/admin/`: controller and service tests.
- `frontend/`: Vue 3 application.
- `frontend/src/api/`: typed HTTP clients.
- `frontend/src/router/`: route definitions and guards.
- `frontend/src/stores/`: Pinia stores for auth, menus, and user state.
- `frontend/src/layouts/`: authenticated admin layout.
- `frontend/src/views/auth/`: login and registration.
- `frontend/src/views/party-hr/`: appointment board.
- `frontend/src/views/general-admin/`: 综合管理部文字提示页.
- `frontend/src/views/settings/`: user and role assignment pages.
- `frontend/src/components/appointment/`: appointment table, form dialog, form grid, photo uploader.
- `deploy/nginx/default.conf`: Nginx static site and API proxy.
- `docker-compose.yml`: MySQL, backend, frontend.
- `.env.example`: deployment environment variables.

## Execution Rules

- Run each task from `D:\AIAPP`.
- Commit after every task if Git is initialized.
- Keep generated frontend and backend code under `backend/` and `frontend/`; leave `素材/`, `需求/`, `原型/`, `.superpowers/`, and `.analysis/` untouched except for reading assets.
- Use the design spec at `docs/superpowers/specs/2026-05-31-group-admin-system-design.md` as the source of truth.

### Task 1: Repository And Tooling Baseline

**Files:**
- Create: `.gitignore`
- Create: `.env.example`
- Create: `README.md`
- Create: `backend/`
- Create: `frontend/`

- [ ] **Step 1: Initialize Git if missing**

Run:

```powershell
if (-not (Test-Path .git)) { git init }
git status --short
```

Expected: `git status --short` runs without `fatal: not a git repository`.

- [ ] **Step 2: Create `.gitignore`**

Create `.gitignore` with:

```gitignore
.idea/
.vscode/
.DS_Store
Thumbs.db

backend/target/
backend/.mvn/wrapper/maven-wrapper.jar

frontend/node_modules/
frontend/dist/
frontend/.vite/

uploads/
.env
.superpowers/
.analysis/
```

- [ ] **Step 3: Create `.env.example`**

Create `.env.example` with:

```dotenv
MYSQL_DATABASE=group_admin
MYSQL_USER=group_admin
MYSQL_PASSWORD=group_admin_password
MYSQL_ROOT_PASSWORD=root_password
JWT_SECRET=replace_with_at_least_32_characters
UPLOAD_DIR=/app/uploads
BACKEND_PORT=8080
FRONTEND_PORT=80
```

- [ ] **Step 4: Create `README.md`**

Create `README.md` with:

```markdown
# 集团后台管理系统

## 技术栈

- 后端：Java、Spring Boot、MySQL
- 前端：Vue 3、TypeScript、Element Plus
- 部署：Docker Compose、Nginx

## 默认账号

- 账号：superadmin
- 初始密码：xjyadmin

## 本地开发入口

- 后端：`backend/`
- 前端：`frontend/`
- 设计说明：`docs/superpowers/specs/2026-05-31-group-admin-system-design.md`
```

- [ ] **Step 5: Commit**

Run:

```powershell
git add .gitignore .env.example README.md
git commit -m "chore: initialize repository baseline"
```

Expected: commit succeeds.

### Task 2: Backend Scaffold And Database Migration

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/company/admin/AdminApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/test/java/com/company/admin/MigrationSmokeTest.java`

- [ ] **Step 1: Generate Spring Boot backend**

Run:

```powershell
$url = "https://start.spring.io/starter.zip?type=maven-project&language=java&baseDir=backend&groupId=com.company&artifactId=admin&name=admin&packageName=com.company.admin&packaging=jar&javaVersion=17&dependencies=web,security,validation,data-jpa,mysql,flyway,lombok"
Invoke-WebRequest -Uri $url -OutFile backend.zip
Expand-Archive backend.zip -DestinationPath .
Remove-Item backend.zip
```

Expected: `backend/pom.xml` exists.

- [ ] **Step 2: Add migration smoke test**

Create `backend/src/test/java/com/company/admin/MigrationSmokeTest.java`:

```java
package com.company.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MigrationSmokeTest {
    @Test
    void contextLoadsWithMigrations() {
    }
}
```

- [ ] **Step 3: Configure test database dependency**

Add Testcontainers dependencies to `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4: Write initial schema and seed data**

Create `backend/src/main/resources/db/migration/V1__init_schema.sql` with tables:

```sql
CREATE TABLE sys_department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(120) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL
);

CREATE TABLE sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  path VARCHAR(160) NOT NULL,
  permission_code VARCHAR(120) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  department_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  last_login_time DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES sys_department(id)
);

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
);

CREATE TABLE appointment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  id_card VARCHAR(32) NOT NULL,
  position_name VARCHAR(100) NOT NULL,
  graduation_school VARCHAR(160) NOT NULL,
  address VARCHAR(255) NOT NULL,
  gender VARCHAR(20) NULL,
  birth_date DATE NULL,
  ethnicity VARCHAR(60) NULL,
  native_place VARCHAR(120) NULL,
  birth_place VARCHAR(120) NULL,
  party_join_date DATE NULL,
  work_start_date DATE NULL,
  health_status VARCHAR(80) NULL,
  technical_position VARCHAR(120) NULL,
  specialty VARCHAR(255) NULL,
  full_time_education VARCHAR(160) NULL,
  full_time_school_major VARCHAR(255) NULL,
  in_service_education VARCHAR(160) NULL,
  in_service_school_major VARCHAR(255) NULL,
  current_position VARCHAR(255) NULL,
  proposed_position VARCHAR(255) NULL,
  proposed_removal_position VARCHAR(255) NULL,
  resume_text TEXT NULL,
  reward_punishment TEXT NULL,
  annual_assessment_result TEXT NULL,
  appointment_reason TEXT NULL,
  reporting_unit TEXT NULL,
  reporting_unit_date DATE NULL,
  approval_authority_opinion TEXT NULL,
  approval_authority_date DATE NULL,
  administrative_appointment_opinion TEXT NULL,
  administrative_appointment_date DATE NULL,
  form_filler VARCHAR(100) NULL,
  photo_file_id BIGINT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE appointment_family_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  appointment_record_id BIGINT NOT NULL,
  relationship VARCHAR(64) NULL,
  name VARCHAR(64) NULL,
  age INT NULL,
  political_status VARCHAR(80) NULL,
  work_unit_and_position VARCHAR(255) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_family_appointment FOREIGN KEY (appointment_record_id) REFERENCES appointment_record(id)
);

CREATE TABLE sys_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  mime_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT NOT NULL,
  business_type VARCHAR(80) NOT NULL,
  business_id BIGINT NULL,
  uploaded_by BIGINT NULL,
  uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO sys_department(code, name, sort_order) VALUES
('PARTY_HR', '党群人力部', 1),
('GENERAL_ADMIN', '综合管理部', 2);

INSERT INTO sys_role(code, name) VALUES
('SUPER_ADMIN', '超级管理员'),
('DEPARTMENT_USER', '部门用户');

INSERT INTO sys_permission(code, name) VALUES
('menu:party-hr', '党群人力部菜单'),
('menu:general-admin', '综合管理部菜单'),
('menu:settings', '设置菜单'),
('appointment:manage', '任免审批管理'),
('system:user-role', '用户角色分配');

INSERT INTO sys_menu(name, path, permission_code, sort_order) VALUES
('党群人力部', '/party-hr', 'menu:party-hr', 1),
('综合管理部', '/general-admin', 'menu:general-admin', 2),
('设置', '/settings/users', 'menu:settings', 3);
```

- [ ] **Step 5: Run migration test**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=MigrationSmokeTest
Set-Location ..
```

Expected: test passes and Flyway migration applies.

- [ ] **Step 6: Commit**

Run:

```powershell
git add backend
git commit -m "feat: add backend scaffold and schema migration"
```

Expected: commit succeeds.

### Task 3: Backend Common Response, Security, And Auth

**Files:**
- Create: `backend/src/main/java/com/company/admin/common/ApiResponse.java`
- Create: `backend/src/main/java/com/company/admin/common/BusinessException.java`
- Create: `backend/src/main/java/com/company/admin/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/company/admin/auth/AuthController.java`
- Create: `backend/src/main/java/com/company/admin/auth/AuthService.java`
- Create: `backend/src/main/java/com/company/admin/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/company/admin/auth/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/company/admin/auth/dto/AuthResponse.java`
- Create: `backend/src/main/java/com/company/admin/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/company/admin/security/JwtService.java`
- Test: `backend/src/test/java/com/company/admin/auth/AuthControllerTest.java`

- [ ] **Step 1: Write auth controller tests**

Create `AuthControllerTest.java`:

```java
package com.company.admin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.admin.auth.dto.LoginRequest;
import com.company.admin.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void superAdminCanLogin() throws Exception {
        LoginRequest request = new LoginRequest("superadmin", "xjyadmin");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("superadmin"))
            .andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"));
    }

    @Test
    void registerDepartmentUser() throws Exception {
        RegisterRequest request = new RegisterRequest("party_user", "P@ssw0rd123", "13800000000", "PARTY_HR");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("party_user"))
            .andExpect(jsonPath("$.data.departmentCode").value("PARTY_HR"));
    }
}
```

- [ ] **Step 2: Run auth tests to verify failure**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=AuthControllerTest
Set-Location ..
```

Expected: fails because auth classes and routes do not exist.

- [ ] **Step 3: Implement common response**

Create `ApiResponse.java`:

```java
package com.company.admin.common;

public record ApiResponse<T>(boolean success, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

Create `BusinessException.java`:

```java
package com.company.admin.common;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

Create `GlobalExceptionHandler.java`:

```java
package com.company.admin.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("请求参数不合法"));
    }
}
```

- [ ] **Step 4: Implement auth DTOs and service contract**

Create the DTO records:

```java
package com.company.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
```

```java
package com.company.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank String password,
    @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @NotBlank String departmentCode
) {}
```

```java
package com.company.admin.auth.dto;

import java.util.List;

public record AuthResponse(
    Long userId,
    String username,
    String phone,
    String departmentCode,
    String departmentName,
    List<String> roles,
    List<String> permissions,
    String token
) {}
```

- [ ] **Step 5: Implement auth controller**

Create `AuthController.java`:

```java
package com.company.admin.auth;

import com.company.admin.auth.dto.AuthResponse;
import com.company.admin.auth.dto.LoginRequest;
import com.company.admin.auth.dto.RegisterRequest;
import com.company.admin.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }
}
```

- [ ] **Step 6: Implement repositories, password encoding, superadmin seed, and JWT**

Create JPA entities and repositories for `sys_user`, `sys_role`, `sys_department`, `sys_permission`, and relationships. `AuthService.login` must:

```java
public AuthResponse login(LoginRequest request) {
    var user = userRepository.findByUsernameAndDeletedFalse(request.username())
        .orElseThrow(() -> new BusinessException("账号或密码错误"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        throw new BusinessException("账号或密码错误");
    }
    return buildAuthResponse(user, jwtService.createToken(user.getUsername()));
}
```

`AuthService.register` must:

```java
public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
        throw new BusinessException("账号已存在");
    }
    var department = departmentRepository.findByCode(request.departmentCode())
        .orElseThrow(() -> new BusinessException("部门不存在"));
    var role = roleRepository.findByCode("DEPARTMENT_USER")
        .orElseThrow(() -> new BusinessException("角色不存在"));
    var user = SysUser.createDepartmentUser(
        request.username(),
        passwordEncoder.encode(request.password()),
        request.phone(),
        department,
        role
    );
    userRepository.save(user);
    return buildAuthResponse(user, jwtService.createToken(user.getUsername()));
}
```

Add a startup runner that creates `superadmin` if missing with `BCryptPasswordEncoder.encode("xjyadmin")` and role `SUPER_ADMIN`.

- [ ] **Step 7: Run auth tests**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=AuthControllerTest
Set-Location ..
```

Expected: both auth tests pass.

- [ ] **Step 8: Commit**

Run:

```powershell
git add backend
git commit -m "feat: add authentication and default admin"
```

Expected: commit succeeds.

### Task 4: Backend Menus, Roles, And Department Authorization

**Files:**
- Create: `backend/src/main/java/com/company/admin/system/MenuController.java`
- Create: `backend/src/main/java/com/company/admin/system/UserController.java`
- Create: `backend/src/main/java/com/company/admin/system/RoleController.java`
- Create: `backend/src/main/java/com/company/admin/system/dto/MenuItemResponse.java`
- Create: `backend/src/main/java/com/company/admin/system/dto/UserSummaryResponse.java`
- Test: `backend/src/test/java/com/company/admin/system/MenuPermissionTest.java`

- [ ] **Step 1: Write menu permission tests**

Create `MenuPermissionTest.java`:

```java
package com.company.admin.system;

import com.company.admin.auth.AuthService;
import com.company.admin.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MenuPermissionTest {
    @Autowired MockMvc mvc;
    @Autowired AuthService authService;

    @Test
    void superAdminReceivesAllMenus() throws Exception {
        String token = authService.login(new LoginRequest("superadmin", "xjyadmin")).token();
        mvc.perform(get("/api/system/menus/current").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("党群人力部", "综合管理部", "设置")));
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=MenuPermissionTest
Set-Location ..
```

Expected: fails because menu endpoint does not exist.

- [ ] **Step 3: Implement current menu endpoint**

Create `MenuItemResponse.java`:

```java
package com.company.admin.system.dto;

public record MenuItemResponse(String name, String path, String permissionCode, int sortOrder) {}
```

Create `MenuController.java`:

```java
package com.company.admin.system;

import com.company.admin.common.ApiResponse;
import com.company.admin.system.dto.MenuItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/current")
    ApiResponse<List<MenuItemResponse>> current() {
        return ApiResponse.ok(menuService.currentUserMenus());
    }
}
```

`MenuService.currentUserMenus()` must return all enabled menus for `SUPER_ADMIN`; for `DEPARTMENT_USER`, return only `/party-hr` for department `PARTY_HR` and only `/general-admin` for department `GENERAL_ADMIN`.

- [ ] **Step 4: Implement user and role assignment endpoints**

Create endpoints:

```text
GET /api/system/users
GET /api/system/roles
PUT /api/system/users/{id}/roles
```

`PUT /api/system/users/{id}/roles` accepts:

```json
{ "roleCodes": ["DEPARTMENT_USER"] }
```

Only `SUPER_ADMIN` can call these endpoints.

- [ ] **Step 5: Run menu and auth tests**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=MenuPermissionTest,AuthControllerTest
Set-Location ..
```

Expected: tests pass.

- [ ] **Step 6: Commit**

Run:

```powershell
git add backend
git commit -m "feat: add menu and role management APIs"
```

Expected: commit succeeds.

### Task 5: Backend Appointment Records And Family Members

**Files:**
- Create: `backend/src/main/java/com/company/admin/appointment/AppointmentController.java`
- Create: `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`
- Create: `backend/src/main/java/com/company/admin/appointment/dto/AppointmentCreateRequest.java`
- Create: `backend/src/main/java/com/company/admin/appointment/dto/AppointmentDetailResponse.java`
- Create: `backend/src/main/java/com/company/admin/appointment/dto/AppointmentListItemResponse.java`
- Create: `backend/src/main/java/com/company/admin/appointment/dto/FamilyMemberRequest.java`
- Test: `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`

- [ ] **Step 1: Write appointment API tests**

Create `AppointmentControllerTest.java`:

```java
package com.company.admin.appointment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.admin.auth.AuthService;
import com.company.admin.auth.dto.LoginRequest;
import com.company.admin.appointment.dto.AppointmentCreateRequest;
import com.company.admin.appointment.dto.FamilyMemberRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AuthService authService;

    @Test
    void superAdminCanCreateAndListAppointment() throws Exception {
        String token = authService.login(new LoginRequest("superadmin", "xjyadmin")).token();
        var request = new AppointmentCreateRequest(
            "张三", "13800000000", "510181199001011234", "综合主管", "四川大学", "都江堰市幸福街道",
            "男", "1990-01-01", "汉族", "四川都江堰", "四川成都",
            "2012-06-01", "2013-07-01", "健康", "", "", "本科", "四川大学公共管理专业",
            "", "", "综合主管", "部门副经理", "", "2013 年至今在集团相关岗位工作。",
            "", "", "", "", null, "此表信息已认定", null, "", null, "填表人A", null,
            List.of(new FamilyMemberRequest("父亲", "张父", 60, "群众", "退休", 1))
        );
        mvc.perform(post("/api/party-hr/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("张三"));

        mvc.perform(get("/api/party-hr/appointments").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].name").value("张三"));
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=AppointmentControllerTest
Set-Location ..
```

Expected: fails because appointment DTOs and endpoints do not exist.

- [ ] **Step 3: Implement appointment DTOs**

Use records with names matching the test. `AppointmentCreateRequest` must contain all form fields from the spec, including `reportingUnitDate`, `approvalAuthorityDate`, `administrativeAppointmentDate`, `formFiller`, `photoFileId`, and `familyMembers`.

- [ ] **Step 4: Implement appointment service**

Service behavior:

```java
public AppointmentDetailResponse create(AppointmentCreateRequest request) {
    permissionService.requirePartyHrAccess();
    AppointmentRecord record = AppointmentRecord.from(request);
    appointmentRepository.save(record);
    familyMemberRepository.saveAll(FamilyMember.from(record, request.familyMembers()));
    return AppointmentDetailResponse.from(record);
}
```

`list` returns only `deleted = false` and maps to table fields: name, phone, idCard, positionName, graduationSchool, address.

`delete` sets `deleted = true` and updates audit fields.

- [ ] **Step 5: Implement controller**

Routes:

```text
GET /api/party-hr/appointments
GET /api/party-hr/appointments/{id}
POST /api/party-hr/appointments
PUT /api/party-hr/appointments/{id}
DELETE /api/party-hr/appointments/{id}
```

- [ ] **Step 6: Run appointment tests**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=AppointmentControllerTest
Set-Location ..
```

Expected: tests pass.

- [ ] **Step 7: Commit**

Run:

```powershell
git add backend
git commit -m "feat: add appointment record APIs"
```

Expected: commit succeeds.

### Task 6: Backend Id Photo Upload

**Files:**
- Create: `backend/src/main/java/com/company/admin/file/FileController.java`
- Create: `backend/src/main/java/com/company/admin/file/FileService.java`
- Create: `backend/src/main/java/com/company/admin/file/dto/FileUploadResponse.java`
- Test: `backend/src/test/java/com/company/admin/file/FileUploadTest.java`

- [ ] **Step 1: Write file upload tests**

Create `FileUploadTest.java`:

```java
package com.company.admin.file;

import com.company.admin.auth.AuthService;
import com.company.admin.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadTest {
    @Autowired MockMvc mvc;
    @Autowired AuthService authService;

    @Test
    void rejectsNonImageFile() throws Exception {
        String token = authService.login(new LoginRequest("superadmin", "xjyadmin")).token();
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());
        mvc.perform(multipart("/api/files/id-photo").file(file).header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=FileUploadTest
Set-Location ..
```

Expected: fails because upload route does not exist.

- [ ] **Step 3: Implement file upload response**

Create `FileUploadResponse.java`:

```java
package com.company.admin.file.dto;

public record FileUploadResponse(Long id, String originalName, String url, long sizeBytes) {}
```

- [ ] **Step 4: Implement upload validation**

`FileService.uploadIdPhoto` must:

```java
if (!List.of("image/jpeg", "image/png").contains(file.getContentType())) {
    throw new BusinessException("仅支持 JPG、JPEG、PNG 图片");
}
if (file.getSize() > 2 * 1024 * 1024) {
    throw new BusinessException("图片大小不能超过 2MB");
}
```

Use `ImageIO.read(file.getInputStream())` to verify the file is an image and calculate ratio. Accept ratio between `0.65` and `0.85`.

- [ ] **Step 5: Run upload test**

Run:

```powershell
Set-Location backend
.\mvnw test -Dtest=FileUploadTest
Set-Location ..
```

Expected: test passes.

- [ ] **Step 6: Commit**

Run:

```powershell
git add backend
git commit -m "feat: add id photo upload validation"
```

Expected: commit succeeds.

### Task 7: Frontend Scaffold, Auth Pages, And Stores

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/views/auth/LoginView.vue`
- Create: `frontend/src/views/auth/RegisterView.vue`
- Test: `frontend/src/views/auth/LoginView.spec.ts`

- [ ] **Step 1: Generate Vue app**

Run:

```powershell
npm create vite@latest frontend -- --template vue-ts
Set-Location frontend
npm install
npm install element-plus @element-plus/icons-vue pinia vue-router axios
npm install -D vitest @vue/test-utils jsdom
Set-Location ..
```

Expected: `frontend/package.json` exists and dependencies install.

- [ ] **Step 2: Write login view test**

Create `frontend/src/views/auth/LoginView.spec.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LoginView from './LoginView.vue'

describe('LoginView', () => {
  it('shows login and register entry', () => {
    const wrapper = mount(LoginView)
    expect(wrapper.text()).toContain('集团后台管理系统')
    expect(wrapper.text()).toContain('登录')
    expect(wrapper.text()).toContain('注册账号')
  })
})
```

- [ ] **Step 3: Run frontend test to verify failure**

Run:

```powershell
Set-Location frontend
npm run test -- LoginView.spec.ts
Set-Location ..
```

Expected: fails because the test script or view does not exist.

- [ ] **Step 4: Configure Vitest**

Add scripts to `frontend/package.json`:

```json
{
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview --host 0.0.0.0",
    "test": "vitest run --environment jsdom"
  }
}
```

- [ ] **Step 5: Implement HTTP client and auth store**

Create `src/api/http.ts`:

```ts
import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

Create `src/stores/auth.ts` with state: `token`, `user`, `menus`, actions: `login`, `register`, `loadCurrentUser`, `logout`.

- [ ] **Step 6: Implement LoginView and RegisterView**

`LoginView.vue` must show:

- Background image from copied asset `frontend/src/assets/group-gate.png`.
- Login form with username and password.
- Login button.
- Register entry text button.

`RegisterView.vue` must show:

- username, password, phone fields.
- department radio group with `党群人力部` and `综合管理部`.

- [ ] **Step 7: Run frontend test**

Run:

```powershell
Set-Location frontend
npm run test -- LoginView.spec.ts
Set-Location ..
```

Expected: test passes.

- [ ] **Step 8: Commit**

Run:

```powershell
git add frontend
git commit -m "feat: add frontend auth pages"
```

Expected: commit succeeds.

### Task 8: Frontend Layout, Route Guards, And Settings

**Files:**
- Create: `frontend/src/layouts/AppLayout.vue`
- Create: `frontend/src/components/PermissionMenu.vue`
- Create: `frontend/src/views/general-admin/GeneralAdminView.vue`
- Create: `frontend/src/views/settings/UserSettingsView.vue`
- Create: `frontend/src/components/settings/RoleAssignDialog.vue`
- Test: `frontend/src/router/routerGuard.spec.ts`

- [ ] **Step 1: Write route guard test**

Create `routerGuard.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { canAccessPath } from './index'

describe('canAccessPath', () => {
  it('allows super admin to access settings', () => {
    expect(canAccessPath('/settings/users', ['SUPER_ADMIN'], 'PARTY_HR')).toBe(true)
  })

  it('blocks department user from settings', () => {
    expect(canAccessPath('/settings/users', ['DEPARTMENT_USER'], 'PARTY_HR')).toBe(false)
  })
})
```

- [ ] **Step 2: Run route guard test to verify failure**

Run:

```powershell
Set-Location frontend
npm run test -- routerGuard.spec.ts
Set-Location ..
```

Expected: fails because `canAccessPath` is not implemented.

- [ ] **Step 3: Implement route guard helper**

Export from `src/router/index.ts`:

```ts
export function canAccessPath(path: string, roles: string[], departmentCode?: string): boolean {
  if (roles.includes('SUPER_ADMIN')) return true
  if (path.startsWith('/settings')) return false
  if (path.startsWith('/party-hr')) return departmentCode === 'PARTY_HR'
  if (path.startsWith('/general-admin')) return departmentCode === 'GENERAL_ADMIN'
  return true
}
```

- [ ] **Step 4: Implement AppLayout and PermissionMenu**

`AppLayout.vue` must contain:

- left sidebar.
- top user display.
- `<router-view />`.

`PermissionMenu.vue` must render backend menu order and navigate by path.

- [ ] **Step 5: Implement settings page**

`UserSettingsView.vue` must:

- call `GET /api/system/users`.
- display username, phone, department, roles.
- open `RoleAssignDialog` on role assignment.

- [ ] **Step 6: Run route tests and build**

Run:

```powershell
Set-Location frontend
npm run test -- routerGuard.spec.ts
npm run build
Set-Location ..
```

Expected: test and build pass.

- [ ] **Step 7: Commit**

Run:

```powershell
git add frontend
git commit -m "feat: add frontend layout and permissions"
```

Expected: commit succeeds.

### Task 9: Frontend Appointment Board And Form Dialog

**Files:**
- Create: `frontend/src/api/appointment.ts`
- Create: `frontend/src/views/party-hr/PartyHrView.vue`
- Create: `frontend/src/components/appointment/AppointmentTable.vue`
- Create: `frontend/src/components/appointment/AppointmentFormDialog.vue`
- Create: `frontend/src/components/appointment/AppointmentFormGrid.vue`
- Create: `frontend/src/components/appointment/PhotoUploader.vue`
- Test: `frontend/src/components/appointment/AppointmentFormGrid.spec.ts`

- [ ] **Step 1: Write form grid test**

Create `AppointmentFormGrid.spec.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppointmentFormGrid from './AppointmentFormGrid.vue'

describe('AppointmentFormGrid', () => {
  it('renders confirmed labels and default approval text', () => {
    const wrapper = mount(AppointmentFormGrid, { props: { readonly: false, modelValue: {} } })
    expect(wrapper.text()).toContain('任免审批表')
    expect(wrapper.text()).toContain('熟悉专业有何专长')
    expect(wrapper.text()).toContain('此表信息已认定')
    expect(wrapper.text()).not.toContain('（盖章）')
    expect(wrapper.text()).not.toContain('计算机')
  })
})
```

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
Set-Location frontend
npm run test -- AppointmentFormGrid.spec.ts
Set-Location ..
```

Expected: fails because appointment components do not exist.

- [ ] **Step 3: Implement appointment API client**

Create functions:

```ts
export function fetchAppointments(params: { page: number; size: number }) {}
export function fetchAppointmentDetail(id: number) {}
export function createAppointment(payload: AppointmentFormPayload) {}
export function updateAppointment(id: number, payload: AppointmentFormPayload) {}
export function deleteAppointment(id: number) {}
```

Each function must call the matching `/api/party-hr/appointments` endpoint through `http`.

- [ ] **Step 4: Implement AppointmentTable**

Columns:

```ts
['姓名', '电话', '身份证号', '职位', '毕业院校', '地址', '操作']
```

Name, edit, and delete are text buttons. Delete opens `ElMessageBox.confirm`.

- [ ] **Step 5: Implement AppointmentFormGrid**

Use Element Plus components:

- `el-input` for short cells.
- `el-date-picker` for dates.
- `el-input type="textarea"` for 简历、奖惩情况、年度考核结果、任免理由、呈报单位、审批机关意见、行政机关任免意见。
- `PhotoUploader` in the top-right photo cell.
- default `approvalAuthorityOpinion` value: `此表信息已认定`.

Do not render these strings anywhere in the grid:

```text
（岁）
计算机
无
（盖章）
```

- [ ] **Step 6: Implement PartyHrView**

`PartyHrView.vue` must:

- fetch list on mount.
- open readonly dialog from name click.
- open editable dialog from edit click.
- refresh list after save or delete.

- [ ] **Step 7: Run component tests and build**

Run:

```powershell
Set-Location frontend
npm run test -- AppointmentFormGrid.spec.ts
npm run build
Set-Location ..
```

Expected: test and build pass.

- [ ] **Step 8: Commit**

Run:

```powershell
git add frontend
git commit -m "feat: add appointment board UI"
```

Expected: commit succeeds.

### Task 10: Docker Compose Deployment

**Files:**
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `deploy/nginx/default.conf`
- Create: `docker-compose.yml`

- [ ] **Step 1: Create backend Dockerfile**

Create `backend/Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 2: Create frontend Dockerfile**

Create `frontend/Dockerfile`:

```dockerfile
FROM node:20-alpine AS build
WORKDIR /workspace
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

- [ ] **Step 3: Create Nginx config**

Create `deploy/nginx/default.conf`:

```nginx
server {
  listen 80;
  server_name _;

  root /usr/share/nginx/html;
  index index.html;

  location /api/ {
    proxy_pass http://backend:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }

  location / {
    try_files $uri $uri/ /index.html;
  }
}
```

- [ ] **Step 4: Create docker-compose.yml**

Create `docker-compose.yml`:

```yaml
services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend:
    build: ./backend
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      UPLOAD_DIR: ${UPLOAD_DIR}
    volumes:
      - uploads:${UPLOAD_DIR}

  frontend:
    build:
      context: .
      dockerfile: frontend/Dockerfile
    depends_on:
      - backend
    ports:
      - "${FRONTEND_PORT}:80"

volumes:
  mysql-data:
  uploads:
```

- [ ] **Step 5: Run deployment build**

Run:

```powershell
Copy-Item .env.example .env
docker compose build
```

Expected: backend and frontend images build.

- [ ] **Step 6: Start deployment**

Run:

```powershell
docker compose up -d
docker compose ps
```

Expected: `mysql`, `backend`, and `frontend` are running.

- [ ] **Step 7: Commit**

Run:

```powershell
git add backend/Dockerfile frontend/Dockerfile deploy docker-compose.yml .env.example
git commit -m "feat: add docker deployment"
```

Expected: commit succeeds.

### Task 11: End-To-End Verification

**Files:**
- Create: `frontend/e2e/group-admin.spec.ts`
- Modify: `frontend/package.json`

- [ ] **Step 1: Install Playwright**

Run:

```powershell
Set-Location frontend
npm install -D @playwright/test
npx playwright install chromium
Set-Location ..
```

Expected: Playwright installs Chromium.

- [ ] **Step 2: Add e2e script**

Add to `frontend/package.json`:

```json
{
  "scripts": {
    "e2e": "playwright test"
  }
}
```

- [ ] **Step 3: Write superadmin e2e test**

Create `frontend/e2e/group-admin.spec.ts`:

```ts
import { expect, test } from '@playwright/test'

test('superadmin can login and see all menus', async ({ page }) => {
  await page.goto('http://localhost')
  await page.getByPlaceholder('请输入账号').fill('superadmin')
  await page.getByPlaceholder('请输入密码').fill('xjyadmin')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByText('党群人力部')).toBeVisible()
  await expect(page.getByText('综合管理部')).toBeVisible()
  await expect(page.getByText('设置')).toBeVisible()
})
```

- [ ] **Step 4: Run e2e test**

Run:

```powershell
Set-Location frontend
npm run e2e
Set-Location ..
```

Expected: e2e test passes against Docker Compose deployment.

- [ ] **Step 5: Run full verification**

Run:

```powershell
Set-Location backend
.\mvnw test
Set-Location ..\frontend
npm run test
npm run build
npm run e2e
Set-Location ..
docker compose ps
```

Expected:

```text
Backend tests pass
Frontend unit tests pass
Frontend build passes
Playwright e2e passes
Docker services remain running
```

- [ ] **Step 6: Commit**

Run:

```powershell
git add frontend
git commit -m "test: add end-to-end verification"
```

Expected: commit succeeds.

## Self-Review

- Spec coverage: The plan covers repository setup, backend schema, default admin, registration, login, menus, role assignment, department authorization, appointment CRUD, family members, id photo upload, frontend auth, layout, route guards, settings, appointment table, appointment form, Docker deployment, and end-to-end validation.
- Placeholder scan: No planned step contains unresolved markers.
- Type consistency: Backend DTO names, endpoint paths, frontend API names, and route paths match the design spec.
- Scope: This plan implements the confirmed first version. Future workflow, export, notifications, organization tree management, and log query screens remain outside this first version by design.
