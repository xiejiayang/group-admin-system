-- 综合管理部管理员角色原地改码，保留角色主键、权限关系及已有用户绑定。
UPDATE sys_role
SET code = 'GENERAL_ADMIN_ADMIN'
WHERE code = 'GENERAL_ADMIN_MANAGER';

-- DEPARTMENT_USER 已被部门 USER 角色替代，删除角色前先清理所有关联关系。
DELETE FROM sys_user_role
WHERE role_id IN (
  SELECT id FROM sys_role WHERE code = 'DEPARTMENT_USER'
);

DELETE FROM sys_role_permission
WHERE role_id IN (
  SELECT id FROM sys_role WHERE code = 'DEPARTMENT_USER'
);

DELETE FROM sys_role
WHERE code = 'DEPARTMENT_USER';
