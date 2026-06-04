package com.company.admin.system;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class SystemControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void superadminMenusContainAllItemsInSortOrder() throws Exception {
        String token = loginSuperadmin().token();

        mockMvc.perform(get("/api/system/menus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("党群人力部"))
                .andExpect(jsonPath("$.data[1].name").value("综合管理部"))
                .andExpect(jsonPath("$.data[2].name").value("设置"));
    }

    @Test
    void partyHrUserMenusOnlyContainPartyHrMenu() throws Exception {
        AuthPayload user = registerUser("task4_party_hr", "PARTY_HR");

        mockMvc.perform(get("/api/system/menus")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("党群人力部"));
    }

    @Test
    void generalAdminUserMenusOnlyContainGeneralAdminMenu() throws Exception {
        AuthPayload user = registerUser("task4_general_admin", "GENERAL_ADMIN");

        mockMvc.perform(get("/api/system/menus")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("综合管理部"));
    }

    @Test
    void partyHrAdminMenusContainDepartmentAndSettingsMenus() throws Exception {
        AuthPayload admin = registerUser("task4_party_admin_menu", "PARTY_HR");
        grantRoles(admin.id(), "PARTY_HR_ADMIN");

        mockMvc.perform(get("/api/system/menus")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].path", hasItem("/party-hr")))
                .andExpect(jsonPath("$.data[*].path", hasItem("/settings/users")))
                .andExpect(jsonPath("$.data[*].path", not(hasItem("/general-admin"))));
    }

    @Test
    void partyHrAdminOnlyReadsPartyHrUsers() throws Exception {
        AuthPayload admin = registerUser("task4_party_admin_users", "PARTY_HR");
        AuthPayload partyUser = registerUser("task4_party_visible", "PARTY_HR");
        AuthPayload generalUser = registerUser("task4_general_hidden", "GENERAL_ADMIN");
        grantRoles(admin.id(), "PARTY_HR_ADMIN");

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].username", hasItem(admin.username())))
                .andExpect(jsonPath("$.data[*].username", hasItem(partyUser.username())))
                .andExpect(jsonPath("$.data[*].username", not(hasItem(generalUser.username()))));
    }

    @Test
    void partyHrAdminRolesForDepartmentUserOnlyReturnPartyHrAdminAndUser() throws Exception {
        AuthPayload admin = registerUser("task4_party_admin_roles", "PARTY_HR");
        AuthPayload target = registerUser("task4_party_role_target", "PARTY_HR");
        grantRoles(admin.id(), "PARTY_HR_ADMIN");

        mockMvc.perform(get("/api/system/roles")
                        .param("targetUserId", String.valueOf(target.id()))
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].code", hasItem("PARTY_HR_ADMIN")))
                .andExpect(jsonPath("$.data[*].code", hasItem("PARTY_HR_USER")))
                .andExpect(jsonPath("$.data[*].code", not(hasItem("SUPER_ADMIN"))))
                .andExpect(jsonPath("$.data[*].code", not(hasItem("GENERAL_ADMIN_MANAGER"))))
                .andExpect(jsonPath("$.data[*].code", not(hasItem("GENERAL_ADMIN_USER"))))
                .andExpect(jsonPath("$.data[*].code", not(hasItem("DEPARTMENT_USER"))));
    }

    @Test
    void departmentAdminCannotAssignRolesToCrossDepartmentUser() throws Exception {
        AuthPayload admin = registerUser("task4_party_admin_cross", "PARTY_HR");
        AuthPayload target = registerUser("task4_general_cross_target", "GENERAL_ADMIN");
        grantRoles(admin.id(), "PARTY_HR_ADMIN");

        mockMvc.perform(put("/api/system/users/{userId}/roles", target.id())
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("GENERAL_ADMIN_USER")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ordinaryUserCannotReadSystemUsers() throws Exception {
        AuthPayload user = registerUser("task4_forbidden", "PARTY_HR");

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void superadminCanReadUsersAndRoles() throws Exception {
        AuthPayload superadmin = loginSuperadmin();

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + superadmin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].id", hasItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].username", hasItem("superadmin")))
                .andExpect(jsonPath("$.data[*].phone", hasItem("00000000000")))
                .andExpect(jsonPath("$.data[*].roles", hasItem(hasItem("SUPER_ADMIN"))))
                .andExpect(jsonPath("$.data[*].status", hasItem("ENABLED")));

        mockMvc.perform(get("/api/system/roles")
                        .param("targetUserId", String.valueOf(superadmin.id()))
                        .header("Authorization", "Bearer " + superadmin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[*].id", hasItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].code", hasItem("SUPER_ADMIN")))
                .andExpect(jsonPath("$.data[0].code").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data[*].name", hasItem("超级管理员")));
        mockMvc.perform(get("/api/system/logs")
                        .header("Authorization", "Bearer " + superadmin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].operatorUsername", hasItem("superadmin")));
    }

    @Test
    void superadminCanAssignCompatibleRolesToOrdinaryUser() throws Exception {
        String token = loginSuperadmin().token();
        AuthPayload user = registerUser("task4_assign", "PARTY_HR");

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("DEPARTMENT_USER", "PARTY_HR_USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.id()))
                .andExpect(jsonPath("$.data.roles", hasItem("DEPARTMENT_USER")))
                .andExpect(jsonPath("$.data.roles", hasItem("PARTY_HR_USER")))
                .andExpect(jsonPath("$.data.roles", not(hasItem("GENERAL_ADMIN_USER"))));
    }

    @Test
    void assignRolesCreatesOperationLog() throws Exception {
        String token = loginSuperadmin().token();
        AuthPayload user = registerUser("task4_assign_log", "PARTY_HR");

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("PARTY_HR_ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/system/logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].operationRecord", hasItem(containsString(user.username()))));
    }

    @Test
    void cannotAssignCrossDepartmentRoleToOrdinaryUser() throws Exception {
        String token = loginSuperadmin().token();
        AuthPayload user = registerUser("task4_cross_assign", "PARTY_HR");

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("DEPARTMENT_USER", "GENERAL_ADMIN_USER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cannotAssignBothDepartmentSpecificRolesToOrdinaryUser() throws Exception {
        String token = loginSuperadmin().token();
        AuthPayload user = registerUser("task4_mixed_assign", "GENERAL_ADMIN");

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of(
                                        "DEPARTMENT_USER",
                                        "PARTY_HR_USER",
                                        "GENERAL_ADMIN_USER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ordinaryUserCannotReadSystemRoles() throws Exception {
        AuthPayload user = registerUser("task4_roles_forbidden", "GENERAL_ADMIN");

        mockMvc.perform(get("/api/system/roles")
                        .param("targetUserId", String.valueOf(user.id()))
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ordinaryUserCannotReadOperationLogs() throws Exception {
        AuthPayload user = registerUser("task4_logs_forbidden", "PARTY_HR");

        mockMvc.perform(get("/api/system/logs")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ordinaryUserCannotAssignRoles() throws Exception {
        AuthPayload user = registerUser("task4_assign_forbidden", "PARTY_HR");

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.id())
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("DEPARTMENT_USER", "PARTY_HR_USER")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cannotAssignUnknownRole() throws Exception {
        String token = loginSuperadmin().token();
        AuthPayload user = registerUser("task4_unknown_role", "GENERAL_ADMIN");

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("DEPARTMENT_USER", "UNKNOWN_ROLE")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cannotRemoveLastSuperadminRole() throws Exception {
        AuthPayload superadmin = loginSuperadmin();

        mockMvc.perform(put("/api/system/users/{userId}/roles", superadmin.id())
                        .header("Authorization", "Bearer " + superadmin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("DEPARTMENT_USER")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private AuthPayload loginSuperadmin() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "superadmin",
                                "password", "xjyadmin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return payload(response);
    }

    private AuthPayload registerUser(String prefix, String departmentCode) throws Exception {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "realName", username,
                                "password", "StrongPass123",
                                "phone", "13800138000",
                                "departmentCode", departmentCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return payload(response);
    }

    private void grantRoles(Long userId, String... roleCodes) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        for (String roleCode : roleCodes) {
            jdbcTemplate.update(
                    """
                    INSERT INTO sys_user_role(user_id, role_id)
                    SELECT ?, id
                    FROM sys_role
                    WHERE code = ?
                    """,
                    userId,
                    roleCode);
        }
    }

    private AuthPayload payload(String response) throws Exception {
        JsonNode data = objectMapper.readTree(response).path("data");
        return new AuthPayload(
                data.path("userId").asLong(),
                data.path("username").asText(),
                data.path("token").asText());
    }

    private record AuthPayload(Long id, String username, String token) {
    }
}
