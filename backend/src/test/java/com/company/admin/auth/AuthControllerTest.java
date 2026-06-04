package com.company.admin.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AuthControllerTest {

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
    void superadminCanLoginWithBcryptPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "superadmin",
                                "password", "xjyadmin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("superadmin"))
                .andExpect(jsonPath("$.data.roles", hasItem("SUPER_ADMIN")))
                .andExpect(jsonPath("$.data.permissions", hasItem("menu:settings")))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void departmentUserCanRegisterAndReadCurrentUserWithReturnedToken() throws Exception {
        String username = uniqueUsername("party_hr_user");

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "realName", username,
                                "password", "StrongPass123",
                                "phone", "13800138000",
                                "departmentCode", "PARTY_HR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.departmentCode").value("PARTY_HR"))
                .andExpect(jsonPath("$.data.roles", hasItem("DEPARTMENT_USER")))
                .andExpect(jsonPath("$.data.roles", hasItem("PARTY_HR_USER")))
                .andExpect(jsonPath("$.data.permissions", hasItem("menu:party-hr")))
                .andExpect(jsonPath("$.data.permissions", hasItem("appointment:manage")))
                .andExpect(jsonPath("$.data.permissions", not(hasItem("menu:general-admin"))))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerBody);
        String token = registerJson.path("data").path("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.departmentCode").value("PARTY_HR"))
                .andExpect(jsonPath("$.data.roles", hasItem("DEPARTMENT_USER")))
                .andExpect(jsonPath("$.data.roles", hasItem("PARTY_HR_USER")))
                .andExpect(jsonPath("$.data.permissions", hasItem("menu:party-hr")))
                .andExpect(jsonPath("$.data.permissions", hasItem("appointment:manage")))
                .andExpect(jsonPath("$.data.permissions", not(hasItem("menu:general-admin"))))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void generalAdminUserOnlyReceivesGeneralAdminPermissions() throws Exception {
        String username = uniqueUsername("general_admin_user");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "realName", username,
                                "password", "StrongPass123",
                                "phone", "13700137000",
                                "departmentCode", "GENERAL_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.departmentCode").value("GENERAL_ADMIN"))
                .andExpect(jsonPath("$.data.roles", hasItem("DEPARTMENT_USER")))
                .andExpect(jsonPath("$.data.roles", hasItem("GENERAL_ADMIN_USER")))
                .andExpect(jsonPath("$.data.permissions", hasItem("menu:general-admin")))
                .andExpect(jsonPath("$.data.permissions", not(hasItem("menu:party-hr"))))
                .andExpect(jsonPath("$.data.permissions", not(hasItem("appointment:manage"))))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void loginWithWrongPasswordReturnsSafeError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "superadmin",
                                "password", "wrong-password"))))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("用户名或密码错误")));
    }

    @Test
    void duplicateUsernameRegistrationReturnsBusinessError() throws Exception {
        String username = uniqueUsername("duplicate_user");
        Map<String, String> request = Map.of(
                "username", username,
                "realName", username,
                "password", "StrongPass123",
                "phone", "13900139000",
                "departmentCode", "GENERAL_ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("用户名已存在")));
    }

    @Test
    void registerWithTooLongUsernameReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "u".repeat(65),
                                "realName", "too_long_username_user",
                                "password", "StrongPass123",
                                "phone", "13500135000",
                                "departmentCode", "PARTY_HR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("长度不能超过64个字符")))
                .andExpect(jsonPath("$.message", not(containsString("用户名已存在"))));
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
