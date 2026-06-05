# UI Draft and Role Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复侧栏与分页显示，完善新增任免草稿生命周期，并将角色模型清理为五个明确角色。

**Architecture:** 前端在现有组件内维护新增草稿并通过 Element Plus 配置调整分页文案；后端通过新增 Flyway V5 迁移清理旧角色数据，并同步收紧注册和角色分配逻辑。保持现有部门访问策略和 API 路径不变。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vitest、Java 17、Spring Boot、Flyway、MySQL、JUnit 5

---

### Task 1: 修复侧栏、分页与新增任免草稿

**Files:**
- Modify: `frontend/src/styles/main.css`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/views/party-hr/PartyHrView.vue`
- Modify: `frontend/src/components/appointment/AppointmentFormDialog.vue`
- Create: `frontend/src/components/appointment/AppointmentFormDialog.spec.ts`

- [ ] **Step 1: 编写失败测试**

新增组件测试，验证取消关闭后重新打开仍保留新增数据，新增保存成功后重新打开恢复空表。

- [ ] **Step 2: 运行测试确认失败**

Run: `npm --prefix frontend run test -- --run frontend/src/components/appointment/AppointmentFormDialog.spec.ts`

Expected: 当前实现重新打开新增弹窗时清空数据，草稿保留断言失败。

- [ ] **Step 3: 实现最小修改**

在 `AppointmentFormDialog.vue` 增加独立 `createDraft`，只在新增保存成功后重置；在 `App.vue` 提供定制 Element Plus 中文分页配置；覆盖 `.app-sidebar.el-aside` 的溢出样式。

- [ ] **Step 4: 运行前端测试**

Run: `npm --prefix frontend run test -- --run`

Expected: 全部测试通过。

### Task 2: 清理角色模型并修复角色分配

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__cleanup_department_roles.sql`
- Modify: `backend/src/main/java/com/company/admin/system/DepartmentAccessPolicy.java`
- Modify: `backend/src/main/java/com/company/admin/system/SystemService.java`
- Modify: `backend/src/main/java/com/company/admin/auth/AuthService.java`
- Modify: `backend/src/test/java/com/company/admin/MigrationSmokeTest.java`
- Modify: `backend/src/test/java/com/company/admin/auth/AuthServiceTest.java`
- Modify: `backend/src/test/java/com/company/admin/auth/AuthControllerTest.java`
- Modify: `backend/src/test/java/com/company/admin/system/SystemServiceTest.java`
- Modify: `backend/src/test/java/com/company/admin/system/SystemControllerTest.java`
- Modify: `backend/src/test/java/com/company/admin/system/OperationLogServiceTest.java`
- Modify: `frontend/src/components/settings/RoleAssignDialog.vue`
- Modify: `frontend/src/components/settings/RoleAssignDialog.spec.ts`
- Modify: `frontend/src/router/access.ts`
- Modify: `frontend/src/router/routerGuard.spec.ts`

- [ ] **Step 1: 编写和更新失败测试**

测试注册账号仅含部门 `USER` 角色；角色分配结果不含 `DEPARTMENT_USER`；综合管理部管理员使用 `GENERAL_ADMIN_ADMIN`；角色弹窗只初始化后端返回的可见角色。

- [ ] **Step 2: 运行定向测试确认失败**

Run: `cd backend && .\mvnw.cmd -Dtest=AuthServiceTest,SystemServiceTest,MigrationSmokeTest test`

Run: `npm --prefix frontend run test -- --run frontend/src/components/settings/RoleAssignDialog.spec.ts frontend/src/router/routerGuard.spec.ts`

Expected: 旧注册和角色补齐逻辑、旧管理员编码导致断言失败。

- [ ] **Step 3: 新增 V5 迁移并修改业务逻辑**

迁移重命名 `GENERAL_ADMIN_MANAGER`，删除 `DEPARTMENT_USER` 关系和角色。注册只添加部门用户角色；角色策略只保留本部门管理员和用户角色；角色弹窗从后端角色列表过滤当前选中项。

- [ ] **Step 4: 运行后端和前端完整测试**

Run: `cd backend && .\mvnw.cmd test`

Run: `npm --prefix frontend run test -- --run`

Expected: 后端和前端全部测试通过。

### Task 3: 构建、部署与浏览器验证

**Files:**
- No production file changes expected

- [ ] **Step 1: 构建前端**

Run: `npm --prefix frontend run build`

Expected: 构建成功。

- [ ] **Step 2: 重建 Docker 环境**

Run: `docker compose up -d --build`

Expected: MySQL 应用 V5，前后端容器正常运行。

- [ ] **Step 3: 浏览器验证**

验证侧栏 `scrollWidth === clientWidth`、分页中文、取消后草稿恢复、保存成功关闭并清空，以及超级管理员给部门管理员分配角色成功。
