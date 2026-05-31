CREATE TABLE sys_department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_department_code UNIQUE (code)
);

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(120) NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_permission_code UNIQUE (code)
);

CREATE TABLE sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  path VARCHAR(160) NOT NULL,
  -- 菜单权限码允许为空，便于后续扩展纯分组菜单；非空值必须引用权限字典，避免菜单与 RBAC 权限脱节。
  permission_code VARCHAR(120) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_menu_path UNIQUE (path)
);

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  department_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  last_login_time DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uk_sys_user_username UNIQUE (username),
  CONSTRAINT fk_sys_user_department FOREIGN KEY (department_id) REFERENCES sys_department(id)
);

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
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
  -- 任免表照片通过文件表统一管理，空值表示尚未上传照片。
  photo_file_id BIGINT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_appointment_family_record FOREIGN KEY (appointment_record_id) REFERENCES appointment_record(id)
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

CREATE INDEX idx_sys_user_department_id ON sys_user(department_id);
CREATE INDEX idx_sys_user_deleted ON sys_user(deleted);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id);
CREATE INDEX idx_sys_role_permission_permission_id ON sys_role_permission(permission_id);
CREATE INDEX idx_sys_menu_permission_code ON sys_menu(permission_code);
CREATE INDEX idx_appointment_record_photo_file_id ON appointment_record(photo_file_id);
CREATE INDEX idx_appointment_record_deleted ON appointment_record(deleted);
CREATE INDEX idx_appointment_family_record_id ON appointment_family_member(appointment_record_id);
CREATE INDEX idx_sys_file_business ON sys_file(business_type, business_id);
CREATE INDEX idx_sys_file_deleted ON sys_file(deleted);

-- 关键领域外键：菜单权限码和任免照片文件均由数据库兜底保护，支撑权限配置、照片上传和逻辑删除场景的二次开发。
ALTER TABLE sys_menu
  ADD CONSTRAINT fk_sys_menu_permission_code FOREIGN KEY (permission_code) REFERENCES sys_permission(code);

ALTER TABLE appointment_record
  ADD CONSTRAINT fk_appointment_record_photo_file FOREIGN KEY (photo_file_id) REFERENCES sys_file(id);

-- 部门初始化：第一版固定内置党群人力部和综合管理部，后续新增部门时应同步扩展菜单和权限。
INSERT INTO sys_department(code, name, sort_order) VALUES
('PARTY_HR', '党群人力部', 1),
('GENERAL_ADMIN', '综合管理部', 2);

-- 角色初始化：超级管理员用于系统管理，部门用户用于普通部门账号的基础授权。
INSERT INTO sys_role(code, name) VALUES
('SUPER_ADMIN', '超级管理员'),
('DEPARTMENT_USER', '部门用户');

-- 默认超级管理员仅用于系统首版初始化，生产环境上线后应及时修改密码；密码为 xjyadmin 的 BCrypt 哈希，禁止写入明文密码。
INSERT INTO sys_user(username, password_hash, phone, status) VALUES
('superadmin', '$2a$10$olR149mDbEaGg4Nv5yB3ceiUgmmBBMRpk0OwPgOy1y382WAG555RK', '00000000000', 'ENABLED');

INSERT INTO sys_user_role(user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'superadmin';

-- 权限初始化：权限码作为后端鉴权和前端菜单过滤的稳定标识，业务逻辑不要依赖中文名称。
INSERT INTO sys_permission(code, name, description) VALUES
('menu:party-hr', '党群人力部菜单', '访问党群人力部看板'),
('menu:general-admin', '综合管理部菜单', '访问综合管理部页面'),
('menu:settings', '设置菜单', '访问系统设置页面'),
('appointment:manage', '任免审批管理', '管理任免审批记录'),
('system:user-role', '用户角色分配', '维护用户角色关系');

-- 菜单初始化：菜单由后端返回给前端渲染，路径需与前端路由保持一致。
INSERT INTO sys_menu(name, path, permission_code, sort_order) VALUES
('党群人力部', '/party-hr', 'menu:party-hr', 1),
('综合管理部', '/general-admin', 'menu:general-admin', 2),
('设置', '/settings/users', 'menu:settings', 3);

-- 角色权限初始化：超级管理员默认拥有全部权限，部门用户默认拥有部门菜单和任免管理基础权限。
INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.code = 'SUPER_ADMIN';

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('menu:party-hr', 'menu:general-admin', 'appointment:manage')
WHERE r.code = 'DEPARTMENT_USER';
