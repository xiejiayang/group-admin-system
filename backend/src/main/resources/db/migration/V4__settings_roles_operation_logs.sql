ALTER TABLE sys_user
  ADD COLUMN real_name VARCHAR(64) NOT NULL DEFAULT '';

UPDATE sys_user
SET real_name = username
WHERE real_name = '';

-- 部门管理员角色用于承载本部门业务菜单、用户角色管理和操作日志查看能力。
INSERT INTO sys_role(code, name)
SELECT 'PARTY_HR_ADMIN', '党群人力部管理员'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role WHERE code = 'PARTY_HR_ADMIN'
);

INSERT INTO sys_role(code, name)
SELECT 'GENERAL_ADMIN_MANAGER', '综合管理部管理员'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role WHERE code = 'GENERAL_ADMIN_MANAGER'
);

-- 系统操作日志权限单独维护，便于后续在设置页控制审计入口。
INSERT INTO sys_permission(code, name, description)
SELECT 'system:operation-log', '系统日志', '查看系统操作记录'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE code = 'system:operation-log'
);

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('menu:settings', 'system:user-role', 'system:operation-log')
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN (
  'menu:party-hr',
  'appointment:manage',
  'menu:settings',
  'system:user-role',
  'system:operation-log'
)
WHERE r.code = 'PARTY_HR_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN (
  'menu:general-admin',
  'menu:settings',
  'system:user-role',
  'system:operation-log'
)
WHERE r.code = 'GENERAL_ADMIN_MANAGER'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );

-- 操作日志保存操作者快照，避免后续部门、手机号或角色调整影响历史审计记录。
CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_user_id BIGINT NOT NULL,
  operator_username VARCHAR(64) NOT NULL,
  operator_real_name VARCHAR(64) NOT NULL,
  operator_department_name VARCHAR(100) NULL,
  operator_phone VARCHAR(32) NOT NULL,
  operator_role_names VARCHAR(512) NOT NULL,
  operation_content TEXT NOT NULL,
  operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sys_operation_log_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user(id)
);

CREATE INDEX idx_sys_operation_log_time ON sys_operation_log(operation_time);
CREATE INDEX idx_sys_operation_log_department ON sys_operation_log(operator_department_name);
