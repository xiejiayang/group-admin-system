package com.company.admin.system;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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
    void ordinaryUserCannotReadSystemUsers() throws Exception {
        AuthPayload user = registerUser("task4_forbidden", "PARTY_HR");

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void superadminCanReadUsersAndRoles() throws Exception {
        String token = loginSuperadmin().token();

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].id", hasItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].username", hasItem("superadmin")))
                .andExpect(jsonPath("$.data[*].phone", hasItem("00000000000")))
                .andExpect(jsonPath("$.data[*].roles", hasItem(hasItem("SUPER_ADMIN"))))
                .andExpect(jsonPath("$.data[*].status", hasItem("ENABLED")));

        mockMvc.perform(get("/api/system/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].id", hasItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].code", hasItem("SUPER_ADMIN")))
                .andExpect(jsonPath("$.data[*].name", hasItem("超级管理员")));
    }

    @Test
    void superadminCanAssignRolesToOrdinaryUser() throws Exception {
        String token = loginSuperadmin().token();
        AuthPayload user = registerUser("task4_assign", "PARTY_HR");

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", java.util.List.of("DEPARTMENT_USER", "GENERAL_ADMIN_USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.id()))
                .andExpect(jsonPath("$.data.roles", hasItem("DEPARTMENT_USER")))
                .andExpect(jsonPath("$.data.roles", hasItem("GENERAL_ADMIN_USER")))
                .andExpect(jsonPath("$.data.roles", not(hasItem("PARTY_HR_USER"))));
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
