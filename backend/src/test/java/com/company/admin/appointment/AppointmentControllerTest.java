package com.company.admin.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentControllerTest {

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
        jdbcTemplate.update("DELETE FROM appointment_family_member");
        jdbcTemplate.update("DELETE FROM appointment_record");
        jdbcTemplate.update("DELETE FROM sys_file");
        jdbcTemplate.update("""
                INSERT INTO sys_file(
                    id, original_name, stored_name, storage_path, mime_type, size_bytes, business_type
                ) VALUES (1, 'photo.jpg', 'photo.jpg', '/tmp/photo.jpg', 'image/jpeg', 1024, 'ID_PHOTO')
                """);
        jdbcTemplate.update("""
                INSERT INTO sys_file(
                    id, original_name, stored_name, storage_path, mime_type, size_bytes, business_type, deleted
                ) VALUES
                (2, 'deleted.jpg', 'deleted.jpg', '/tmp/deleted.jpg', 'image/jpeg', 1024, 'ID_PHOTO', TRUE),
                (3, 'other.jpg', 'other.jpg', '/tmp/other.jpg', 'image/jpeg', 1024, 'OTHER', FALSE)
                """);
    }

    @Test
    void superadminCanCreateListReadUpdateAndDeleteAppointmentRecord() throws Exception {
        String token = loginSuperadmin().token();

        Long appointmentId = createAppointment(token, createRequest("张三", "任免理由第一行\n任免理由第二行", List.of(
                familyMember("配偶", "李四", 35, "群众", "集团办公室主任", 1))));

        mockMvc.perform(get("/api/party-hr/appointments")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.items[*].id", hasItem(appointmentId.intValue())))
                .andExpect(jsonPath("$.data.items[*].name", hasItem("张三")))
                .andExpect(jsonPath("$.data.items[*].phone", hasItem("13900001111")))
                .andExpect(jsonPath("$.data.items[*].idCard", hasItem("110101199001011234")))
                .andExpect(jsonPath("$.data.items[*].positionName", hasItem("党群主管")))
                .andExpect(jsonPath("$.data.items[*].graduationSchool", hasItem("中国人民大学")))
                .andExpect(jsonPath("$.data.items[*].address", hasItem("北京市朝阳区")));

        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(appointmentId))
                .andExpect(jsonPath("$.data.gender").value("男"))
                .andExpect(jsonPath("$.data.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.data.ethnicity").value("汉族"))
                .andExpect(jsonPath("$.data.nativePlace").value("山东济南"))
                .andExpect(jsonPath("$.data.birthPlace").value("北京"))
                .andExpect(jsonPath("$.data.partyJoinDate").value("2012-07-01"))
                .andExpect(jsonPath("$.data.workStartDate").value("2013-08-01"))
                .andExpect(jsonPath("$.data.healthStatus").value("健康"))
                .andExpect(jsonPath("$.data.technicalPosition").value("高级政工师"))
                .andExpect(jsonPath("$.data.specialty").value("公共管理"))
                .andExpect(jsonPath("$.data.fullTimeEducation").value("本科"))
                .andExpect(jsonPath("$.data.fullTimeSchoolMajor").value("中国人民大学 行政管理"))
                .andExpect(jsonPath("$.data.inServiceEducation").value("硕士"))
                .andExpect(jsonPath("$.data.inServiceSchoolMajor").value("中央党校 经济管理"))
                .andExpect(jsonPath("$.data.currentPosition").value("党群主管"))
                .andExpect(jsonPath("$.data.proposedPosition").value("党群人力部副部长"))
                .andExpect(jsonPath("$.data.proposedRemovalPosition").value("党群主管"))
                .andExpect(jsonPath("$.data.resumeText").value("2013-2018 任专员\n2018-至今 任主管"))
                .andExpect(jsonPath("$.data.rewardPunishment").value("年度优秀员工"))
                .andExpect(jsonPath("$.data.annualAssessmentResult").value("优秀"))
                .andExpect(jsonPath("$.data.appointmentReason").value("任免理由第一行\n任免理由第二行"))
                .andExpect(jsonPath("$.data.reportingUnit").value("党群人力部"))
                .andExpect(jsonPath("$.data.reportingUnitDate").value("2026-05-20"))
                .andExpect(jsonPath("$.data.approvalAuthorityOpinion").value("此表信息已认定"))
                .andExpect(jsonPath("$.data.approvalAuthorityDate").value("2026-05-21"))
                .andExpect(jsonPath("$.data.administrativeAppointmentOpinion").value("同意任命"))
                .andExpect(jsonPath("$.data.administrativeAppointmentDate").value("2026-05-22"))
                .andExpect(jsonPath("$.data.formFiller").value("王五"))
                .andExpect(jsonPath("$.data.photoFileId").value(1))
                .andExpect(jsonPath("$.data.familyMembers[0].relationship").value("配偶"))
                .andExpect(jsonPath("$.data.familyMembers[0].name").value("李四"))
                .andExpect(jsonPath("$.data.familyMembers[0].age").value(35))
                .andExpect(jsonPath("$.data.familyMembers[0].politicalStatus").value("群众"))
                .andExpect(jsonPath("$.data.familyMembers[0].workUnitAndPosition").value("集团办公室主任"))
                .andExpect(jsonPath("$.data.familyMembers[0].sortOrder").value(1));

        mockMvc.perform(put("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest("张三丰", "更新理由第一行\n更新理由第二行", List.of(
                                familyMember("父亲", "张父", 66, "中共党员", "退休", 1),
                                familyMember("母亲", "张母", 64, "群众", "退休", 2))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("张三丰"))
                .andExpect(jsonPath("$.data.familyMembers[0].name").value("张父"))
                .andExpect(jsonPath("$.data.familyMembers[1].name").value("张母"));

        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张三丰"))
                .andExpect(jsonPath("$.data.appointmentReason").value("更新理由第一行\n更新理由第二行"))
                .andExpect(jsonPath("$.data.familyMembers[0].relationship").value("父亲"))
                .andExpect(jsonPath("$.data.familyMembers[1].relationship").value("母亲"));

        mockMvc.perform(delete("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/party-hr/appointments")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(appointmentId.intValue()))));

        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generalAdminCannotAccessAppointmentApis() throws Exception {
        AuthPayload generalAdmin = registerUser("task5_general", "GENERAL_ADMIN");

        mockMvc.perform(get("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + generalAdmin.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void partyHrUserCanAccessAppointmentApis() throws Exception {
        AuthPayload partyHrUser = registerUser("task5_party", "PARTY_HR");

        mockMvc.perform(get("/api/party-hr/appointments")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + partyHrUser.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void rejectsTooLongAppointmentBaseFieldBeforeDatabaseConstraint() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.put("name", "a".repeat(65));

        mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.message", not(containsString("DataIntegrity"))))
                .andExpect(jsonPath("$.message", not(containsString("constraint"))));
    }

    @Test
    void rejectsTooLongNestedFamilyMemberFieldBeforeDatabaseConstraint() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.put("familyMembers", List.of(
                familyMember("r".repeat(65), "n".repeat(65), 151, "p".repeat(81), "w".repeat(256), -1)));

        mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("familyMembers")))
                .andExpect(jsonPath("$.message", not(containsString("DataIntegrity"))))
                .andExpect(jsonPath("$.message", not(containsString("constraint"))));
    }

    @Test
    void rejectsInvalidPhotoFileIdBeforeForeignKeyConstraint() throws Exception {
        String token = loginSuperadmin().token();

        assertInvalidPhotoFileIdRejected(token, 999L);
        assertInvalidPhotoFileIdRejected(token, 2L);
        assertInvalidPhotoFileIdRejected(token, 3L);
    }

    @Test
    void generalAdminCannotCreateUpdateOrDeleteAppointmentRecord() throws Exception {
        String superadminToken = loginSuperadmin().token();
        Long appointmentId = createAppointment(superadminToken, createRequest("audited-user", "reason", List.of()));
        AuthPayload generalAdmin = registerUser("task5_general_mutation", "GENERAL_ADMIN");

        mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + generalAdmin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mutableCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(put("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + generalAdmin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mutableCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(delete("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + generalAdmin.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void duplicateDeleteReturnsUnifiedNotFoundResponse() throws Exception {
        String token = loginSuperadmin().token();
        Long appointmentId = createAppointment(token, createRequest("delete-twice", "reason", List.of()));

        mockMvc.perform(delete("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateReplacesFamilyMembersWithoutLeavingOldRowsAndWritesAuditColumns() throws Exception {
        AuthPayload superadmin = loginSuperadmin();
        Long appointmentId = createAppointment(superadmin.token(), createRequest("audited-user", "reason", List.of(
                familyMember("spouse", "old-family", 35, "mass", "office", 1))));

        assertAppointmentAudit(appointmentId, superadmin.id(), superadmin.id());

        mockMvc.perform(put("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + superadmin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest("audited-user", "updated", List.of(
                                familyMember("father", "new-family-1", 66, "party", "retired", 1),
                                familyMember("mother", "new-family-2", 64, "mass", "retired", 2))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertAppointmentAudit(appointmentId, superadmin.id(), superadmin.id());
        assertThat(countFamilyMembers(appointmentId)).isEqualTo(2);
        assertThat(countFamilyMembersByName(appointmentId, "old-family")).isZero();
    }

    private Long createAppointment(String token, Map<String, Object> request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void assertInvalidPhotoFileIdRejected(String token, Long photoFileId) throws Exception {
        Map<String, Object> request = mutableCreateRequest();
        request.put("photoFileId", photoFileId);

        mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("照片文件")));
    }

    private void assertAppointmentAudit(Long appointmentId, Long createdBy, Long updatedBy) {
        Map<String, Object> audit = jdbcTemplate.queryForMap(
                "SELECT created_by, updated_by FROM appointment_record WHERE id = ?", appointmentId);

        assertThat(((Number) audit.get("created_by")).longValue()).isEqualTo(createdBy);
        assertThat(((Number) audit.get("updated_by")).longValue()).isEqualTo(updatedBy);
    }

    private int countFamilyMembers(Long appointmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment_family_member WHERE appointment_record_id = ?",
                Integer.class,
                appointmentId);
        return count == null ? 0 : count;
    }

    private int countFamilyMembersByName(Long appointmentId, String name) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment_family_member WHERE appointment_record_id = ? AND name = ?",
                Integer.class,
                appointmentId,
                name);
        return count == null ? 0 : count;
    }

    private Map<String, Object> mutableCreateRequest() {
        return new java.util.LinkedHashMap<>(createRequest("valid-user", "reason", List.of()));
    }

    private Map<String, Object> createRequest(
            String name,
            String appointmentReason,
            List<Map<String, Object>> familyMembers) {
        return Map.ofEntries(
                Map.entry("name", name),
                Map.entry("phone", "13900001111"),
                Map.entry("idCard", "110101199001011234"),
                Map.entry("positionName", "党群主管"),
                Map.entry("graduationSchool", "中国人民大学"),
                Map.entry("address", "北京市朝阳区"),
                Map.entry("gender", "男"),
                Map.entry("birthDate", "1990-01-01"),
                Map.entry("ethnicity", "汉族"),
                Map.entry("nativePlace", "山东济南"),
                Map.entry("birthPlace", "北京"),
                Map.entry("partyJoinDate", "2012-07-01"),
                Map.entry("workStartDate", "2013-08-01"),
                Map.entry("healthStatus", "健康"),
                Map.entry("technicalPosition", "高级政工师"),
                Map.entry("specialty", "公共管理"),
                Map.entry("fullTimeEducation", "本科"),
                Map.entry("fullTimeSchoolMajor", "中国人民大学 行政管理"),
                Map.entry("inServiceEducation", "硕士"),
                Map.entry("inServiceSchoolMajor", "中央党校 经济管理"),
                Map.entry("currentPosition", "党群主管"),
                Map.entry("proposedPosition", "党群人力部副部长"),
                Map.entry("proposedRemovalPosition", "党群主管"),
                Map.entry("resumeText", "2013-2018 任专员\n2018-至今 任主管"),
                Map.entry("rewardPunishment", "年度优秀员工"),
                Map.entry("annualAssessmentResult", "优秀"),
                Map.entry("appointmentReason", appointmentReason),
                Map.entry("reportingUnit", "党群人力部"),
                Map.entry("reportingUnitDate", "2026-05-20"),
                Map.entry("approvalAuthorityDate", "2026-05-21"),
                Map.entry("administrativeAppointmentOpinion", "同意任命"),
                Map.entry("administrativeAppointmentDate", "2026-05-22"),
                Map.entry("formFiller", "王五"),
                Map.entry("photoFileId", 1),
                Map.entry("familyMembers", familyMembers));
    }

    private Map<String, Object> familyMember(
            String relationship,
            String name,
            Integer age,
            String politicalStatus,
            String workUnitAndPosition,
            Integer sortOrder) {
        return Map.of(
                "relationship", relationship,
                "name", name,
                "age", age,
                "politicalStatus", politicalStatus,
                "workUnitAndPosition", workUnitAndPosition,
                "sortOrder", sortOrder);
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
