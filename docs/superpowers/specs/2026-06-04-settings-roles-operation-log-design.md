# 设置菜单、角色分级与系统日志设计

## 背景

当前系统已经具备登录注册、部门菜单、任免看板和超级管理员角色分配能力。本次需求在现有能力上扩展三项内容：设置菜单改为可展开的二级菜单、角色分配升级为三层权限、系统记录登录与关键业务操作日志。同时，用户已确认注册账号需要新增真实“姓名”字段，不能用账号名临时代替。

## 目标

- “设置”作为左侧一级菜单，可展开或收起，二级菜单包含“角色分配”和“系统日志”。
- 新增“系统日志”页面，标题为“操作记录”，使用 `el-table` 展示操作记录。
- 账号资料新增“姓名”，注册、登录返回、用户列表、日志快照均使用该字段。
- 角色分为三级：超级管理员、部门管理员、部门用户。
- 超级管理员可查看全部菜单、页面、账号和日志，并可分配全部下级角色。
- 部门管理员只管理本部门账号，可分配本部门管理员和本部门用户角色。
- 部门用户只访问本部门业务页面，不能访问设置菜单。
- 左侧菜单栏支持收起和展开，右侧内容区域随之释放空间。

## 非目标

- 不把菜单表改造成完整树结构，本次由前端基于后端返回的“设置”菜单生成二级菜单，避免扩大数据库菜单模型改造面。
- 不新增账号停用、删除、个人资料编辑等用户管理功能。
- 不对任免审批表字段和看板字段做本需求以外的调整。
- 不实现复杂日志筛选、导出和分页以外的高级审计能力。

## 角色设计

角色编码与中文名称如下：

| 角色编码 | 中文名称 | 层级 |
| --- | --- | --- |
| `SUPER_ADMIN` | 超级管理员 | 一级 |
| `PARTY_HR_ADMIN` | 党群人力部管理员 | 二级 |
| `GENERAL_ADMIN_MANAGER` | 综合管理部管理员 | 二级 |
| `PARTY_HR_USER` | 党群人力部用户 | 三级 |
| `GENERAL_ADMIN_USER` | 综合管理部用户 | 三级 |

兼容策略：

- 保留现有 `DEPARTMENT_USER`，只作为历史基础角色兼容，不在角色分配弹窗展示。
- 新注册账号默认拥有其所属部门的三级用户角色。
- 历史账号如已有 `DEPARTMENT_USER + PARTY_HR_USER` 或 `DEPARTMENT_USER + GENERAL_ADMIN_USER`，继续可用。
- 超级管理员账号 `superadmin` 的角色仍为 `SUPER_ADMIN`；对该账号点击“分配角色”时，弹窗只展示“超级管理员”，不展示其他可选项。

## 权限规则

后端接口必须按当前登录账号做真实权限裁剪：

- `SUPER_ADMIN`：可查看全部用户、全部可分配角色、全部系统日志。
- `PARTY_HR_ADMIN`：可查看党群人力部账号；可分配 `PARTY_HR_ADMIN`、`PARTY_HR_USER`。
- `GENERAL_ADMIN_MANAGER`：可查看综合管理部账号；可分配 `GENERAL_ADMIN_MANAGER`、`GENERAL_ADMIN_USER`。
- 部门用户：不可访问用户角色分配和系统日志接口。

角色分配时继续保留后端校验，禁止跨部门分配角色，禁止移除系统最后一个超级管理员。

## 菜单与路由

后端菜单权限保留现有 `menu:settings`，新增系统日志所需权限：

- `system:user-role`：角色分配。
- `system:operation-log`：系统日志。

前端路由新增：

- `/settings/users`：角色分配，对应现有 `UserSettingsView.vue`。
- `/settings/logs`：系统日志，对应新增 `SystemLogsView.vue`。

前端菜单渲染规则：

- 后端返回的 `/settings/users` 菜单在前端转换为一级“设置”。
- “设置”下固定展示二级菜单“角色分配”和“系统日志”。
- 用户无 `menu:settings` 或设置权限时不展示“设置”入口。
- 侧边栏收起时只展示图标；展开时展示一级和二级菜单文字。

## 数据模型

新增用户字段：

- `sys_user.real_name VARCHAR(64) NOT NULL`

历史数据迁移：

- 现有用户 `real_name` 使用 `username` 回填。
- 默认 `superadmin` 的姓名为 `superadmin`。

新增系统日志表：

| 字段 | 含义 |
| --- | --- |
| `id` | 主键 |
| `operator_user_id` | 操作人用户 ID |
| `operator_username` | 操作账号快照 |
| `operator_real_name` | 姓名快照 |
| `operator_department_name` | 部门快照 |
| `operator_phone` | 手机号快照 |
| `operator_role_names` | 角色中文名称快照 |
| `operation_content` | 操作记录文本 |
| `operation_time` | 操作时间 |

前端展示时间格式为 `YYYY-MM-DD HH:mm:ss`。

## 操作日志规则

每次操作新增一条日志，不覆盖历史记录。第一版记录以下操作：

- 登录成功：例如 `superadmin 于 2026-06-04 10:03:50 进行登录`。
- 任免审批新增：记录操作账号、时间、被新增人员姓名。
- 任免审批编辑：记录操作账号、时间、被编辑人员姓名。
- 角色分配保存：记录操作账号、时间、目标账号和新角色。

日志写入由后端服务完成，前端只负责查询展示，避免绕过浏览器或刷新页面导致日志缺失。

## 接口设计

- `POST /api/auth/register`：请求体新增 `realName`，返回体同步新增 `realName`。
- `POST /api/auth/login`：登录成功后写入登录日志，返回体同步新增 `realName`。
- `GET /api/system/users`：按当前登录账号返回可管理用户列表。
- `GET /api/system/roles?targetUserId={id}`：按当前登录账号和目标用户返回可分配角色。打开分配弹窗时必须传目标用户 ID。
- `PUT /api/system/users/{userId}/roles`：保存角色分配，并写入角色分配日志。
- `GET /api/system/logs`：返回当前登录账号可查看的系统日志，按操作时间倒序排列。

## 前端改动

- `RegisterView.vue` 增加“姓名”输入项并做必填校验。
- `auth.ts`、`system.ts` 类型增加 `realName` 字段。
- `UserSettingsView.vue` 用户列表增加姓名展示；角色分配仍复用现有弹窗。
- `RoleAssignDialog.vue` 打开时带目标用户 ID 拉取可分配角色；当目标用户是 `superadmin` 时只展示“超级管理员”。
- `PermissionMenu.vue` 支持 `el-sub-menu` 渲染“设置”及二级菜单。
- `AppLayout.vue` 增加侧边栏收起/展开状态和中部箭头按钮。
- 新增 `SystemLogsView.vue`，标题为“操作记录”，表格列为“操作账号、姓名、部门、手机号、角色、操作记录”。

## 后端改动

- `RegisterRequest`、`AuthResponse`、`UserResponse` 增加 `realName`。
- `AuthService.register` 保存真实姓名；`login` 成功后写入登录日志。
- `SystemService.users` 按当前登录账号过滤可见用户。
- `SystemService.roles` 按当前登录账号和目标用户上下文返回可分配角色。
- `SystemService.assignRoles` 按当前登录账号校验角色分配权限，保存后写入操作日志。
- 新增日志实体、仓库、响应 DTO 和查询接口。
- `AppointmentService.create/update` 成功后写入任免新增和编辑日志。

## 错误处理

- 未登录访问系统接口返回现有认证失败错误。
- 部门用户访问设置相关接口返回 403。
- 部门管理员跨部门查看账号、分配角色或查看日志返回 403。
- 角色不存在、停用或不允许分配时返回业务错误。
- 系统日志写入失败不吞掉主业务异常；第一版采用同事务写入，保证操作和日志一致。

## 测试方案

后端测试：

- 注册接口必须校验并返回 `realName`。
- 超级管理员可查看全部用户、角色和日志。
- 部门管理员只能查看本部门用户、角色和日志。
- 部门用户不能访问设置接口。
- `superadmin` 不能被分配为非超级管理员，且仍保留最后超级管理员保护。
- 登录、任免新增、任免编辑、角色分配保存均生成日志。
- Flyway 迁移在 H2 MySQL 模式下通过。

前端测试：

- 注册页姓名必填。
- 路由权限允许部门管理员访问 `/settings/users` 和 `/settings/logs`，拒绝部门用户访问。
- 左侧菜单可渲染“设置”二级菜单。
- 角色分配弹窗对 `superadmin` 只展示“超级管理员”。
- 系统日志页面按要求渲染表头。

## 部署与验证

实现完成后执行：

- 后端测试：`./mvnw test`
- 前端测试：`npm run test:unit -- --run`
- 前端构建：`npm run build`
- Docker 后端重建并启动，触发数据库迁移。
- 打开本地前端 URL，验证注册姓名、设置二级菜单、系统日志、角色分配和侧边栏收起展开。
