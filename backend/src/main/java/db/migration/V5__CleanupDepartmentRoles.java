package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V5__CleanupDepartmentRoles extends BaseJavaMigration {

    private static final String OLD_GENERAL_ADMIN_ROLE = "GENERAL_ADMIN_MANAGER";
    private static final String GENERAL_ADMIN_ROLE = "GENERAL_ADMIN_ADMIN";
    private static final String DEPARTMENT_USER_ROLE = "DEPARTMENT_USER";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Long oldRoleId = findRoleId(connection, OLD_GENERAL_ADMIN_ROLE);
        Long currentRoleId = findRoleId(connection, GENERAL_ADMIN_ROLE);

        if (oldRoleId != null && currentRoleId != null) {
            // 数据库漂移时优先保留旧角色主键，将新角色上的用户和权限去重合并后再删除新角色。
            mergeRelations(connection, "sys_user_role", "user_id", oldRoleId, currentRoleId);
            mergeRelations(connection, "sys_role_permission", "permission_id", oldRoleId, currentRoleId);
            deleteRoleRelations(connection, currentRoleId);
            deleteRole(connection, currentRoleId);
            updateRoleCode(connection, oldRoleId, GENERAL_ADMIN_ROLE);
        } else if (oldRoleId != null) {
            updateRoleCode(connection, oldRoleId, GENERAL_ADMIN_ROLE);
        }

        Long departmentUserRoleId = findRoleId(connection, DEPARTMENT_USER_ROLE);
        if (departmentUserRoleId != null) {
            // 部门基础角色已由两个部门 USER 角色完全替代，关联关系必须先于角色删除。
            deleteRoleRelations(connection, departmentUserRoleId);
            deleteRole(connection, departmentUserRoleId);
        }
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    private Long findRoleId(Connection connection, String roleCode) throws Exception {
        try (PreparedStatement statement =
                        connection.prepareStatement("SELECT id FROM sys_role WHERE code = ?")) {
            statement.setString(1, roleCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("id") : null;
            }
        }
    }

    private void mergeRelations(
            Connection connection,
            String tableName,
            String relationColumn,
            long targetRoleId,
            long sourceRoleId)
            throws Exception {
        Set<Long> targetRelations = new HashSet<>(relationIds(
                connection, tableName, relationColumn, targetRoleId));
        List<Long> sourceRelations = relationIds(
                connection, tableName, relationColumn, sourceRoleId);

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO " + tableName + "(" + relationColumn + ", role_id) VALUES (?, ?)")) {
            for (Long relationId : sourceRelations) {
                if (targetRelations.add(relationId)) {
                    insert.setLong(1, relationId);
                    insert.setLong(2, targetRoleId);
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        }
    }

    private List<Long> relationIds(
            Connection connection,
            String tableName,
            String relationColumn,
            long roleId)
            throws Exception {
        List<Long> relationIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + relationColumn + " FROM " + tableName + " WHERE role_id = ?")) {
            statement.setLong(1, roleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    relationIds.add(resultSet.getLong(relationColumn));
                }
            }
        }
        return relationIds;
    }

    private void deleteRoleRelations(Connection connection, long roleId) throws Exception {
        deleteByRoleId(connection, "sys_user_role", roleId);
        deleteByRoleId(connection, "sys_role_permission", roleId);
    }

    private void deleteByRoleId(Connection connection, String tableName, long roleId) throws Exception {
        try (PreparedStatement statement =
                        connection.prepareStatement("DELETE FROM " + tableName + " WHERE role_id = ?")) {
            statement.setLong(1, roleId);
            statement.executeUpdate();
        }
    }

    private void deleteRole(Connection connection, long roleId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM sys_role WHERE id = ?")) {
            statement.setLong(1, roleId);
            statement.executeUpdate();
        }
    }

    private void updateRoleCode(Connection connection, long roleId, String roleCode) throws Exception {
        try (PreparedStatement statement =
                        connection.prepareStatement("UPDATE sys_role SET code = ? WHERE id = ?")) {
            statement.setString(1, roleCode);
            statement.setLong(2, roleId);
            statement.executeUpdate();
        }
    }
}
