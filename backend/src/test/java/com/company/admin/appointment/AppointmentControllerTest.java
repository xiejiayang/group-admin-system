package com.company.admin.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

    private static final int EXPECTED_AGE_ON_FIXED_CLOCK = 36;

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
                .andExpect(jsonPath("$.data.items[*].globalSequence", hasItem(1)))
                .andExpect(jsonPath("$.data.items[*].displaySequence", hasItem(1)))
                .andExpect(jsonPath("$.data.items[*].companyName", hasItem("集团公司")))
                .andExpect(jsonPath("$.data.items[*].departmentName", hasItem("党群人力部")))
                .andExpect(jsonPath("$.data.items[*].name", hasItem("张三")))
                .andExpect(jsonPath("$.data.items[*].currentPosition", hasItem("党群主管")))
                .andExpect(jsonPath("$.data.items[*].gender", hasItem("男")))
                .andExpect(jsonPath("$.data.items[*].ethnicity", hasItem("汉族")))
                .andExpect(jsonPath("$.data.items[*].idCard", hasItem("110101199001011234")))
                .andExpect(jsonPath("$.data.items[*].age", hasItem(EXPECTED_AGE_ON_FIXED_CLOCK)))
                .andExpect(jsonPath("$.data.items[*].politicalStatus", hasItem("中共党员")))
                .andExpect(jsonPath("$.data.items[*].fullTimeEducation", hasItem("本科")))
                .andExpect(jsonPath("$.data.items[*].fullTimeEducationDegree", hasItem("学士")))
                .andExpect(jsonPath("$.data.items[*].fullTimeSchool", hasItem("中国人民大学")))
                .andExpect(jsonPath("$.data.items[*].fullTimeMajor", hasItem("行政管理")))
                .andExpect(jsonPath("$.data.items[*].partTimeEducation", hasItem("硕士研究生")))
                .andExpect(jsonPath("$.data.items[*].partTimeDegree", hasItem("硕士")))
                .andExpect(jsonPath("$.data.items[*].partTimeSchool", hasItem("中央党校")))
                .andExpect(jsonPath("$.data.items[*].partTimeMajor", hasItem("经济管理")))
                .andExpect(jsonPath("$.data.items[*].technicalPosition", hasItem("高级政工师")))
                .andExpect(jsonPath("$.data.items[*].phone", hasItem("13900001111")))
                .andExpect(jsonPath("$.data.items[*].maritalStatus", hasItem("已婚")))
                .andExpect(jsonPath("$.data.items[*].remark", hasItem("看板备注")))
                .andExpect(jsonPath("$.data.items[0].positionName").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].graduationSchool").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].address").doesNotExist());

        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(appointmentId))
                .andExpect(jsonPath("$.data.globalSequence").value(1))
                .andExpect(jsonPath("$.data.displaySequence").value(1))
                .andExpect(jsonPath("$.data.companyName").value("集团公司"))
                .andExpect(jsonPath("$.data.departmentName").value("党群人力部"))
                .andExpect(jsonPath("$.data.gender").value("男"))
                .andExpect(jsonPath("$.data.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.data.age").value(EXPECTED_AGE_ON_FIXED_CLOCK))
                .andExpect(jsonPath("$.data.ethnicity").value("汉族"))
                .andExpect(jsonPath("$.data.politicalStatus").value("中共党员"))
                .andExpect(jsonPath("$.data.nativePlace").value("山东济南"))
                .andExpect(jsonPath("$.data.birthPlace").value("北京"))
                .andExpect(jsonPath("$.data.partyJoinDate").value("2012-07-01"))
                .andExpect(jsonPath("$.data.workStartDate").value("2013-08-01"))
                .andExpect(jsonPath("$.data.healthStatus").value("健康"))
                .andExpect(jsonPath("$.data.technicalPosition").value("高级政工师"))
                .andExpect(jsonPath("$.data.specialty").value("公共管理"))
                .andExpect(jsonPath("$.data.fullTimeEducation").value("本科"))
                .andExpect(jsonPath("$.data.fullTimeEducationDegree").value("学士"))
                .andExpect(jsonPath("$.data.fullTimeSchool").value("中国人民大学"))
                .andExpect(jsonPath("$.data.fullTimeMajor").value("行政管理"))
                .andExpect(jsonPath("$.data.fullTimeSchoolMajor").value("中国人民大学 行政管理"))
                .andExpect(jsonPath("$.data.inServiceEducation").value("硕士"))
                .andExpect(jsonPath("$.data.inServiceSchoolMajor").value("中央党校 经济管理"))
                .andExpect(jsonPath("$.data.partTimeEducation").value("硕士研究生"))
                .andExpect(jsonPath("$.data.partTimeDegree").value("硕士"))
                .andExpect(jsonPath("$.data.partTimeSchool").value("中央党校"))
                .andExpect(jsonPath("$.data.partTimeMajor").value("经济管理"))
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
                .andExpect(jsonPath("$.data.maritalStatus").value("已婚"))
                .andExpect(jsonPath("$.data.remark").value("看板备注"))
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
    void createAssignsCompanyTypeGlobalSequenceAndBoardDisplaySequenceThenDeleteReordersDisplaySequence() throws Exception {
        String token = loginSuperadmin().token();

        Long firstId = createAppointment(token, createRequest("sequence-first", "reason", List.of()));
        Long secondId = createAppointment(token, createRequest("sequence-second", "reason", List.of()));
        Map<String, Object> otherCompanyRequest = mutableCreateRequest();
        otherCompanyRequest.put("name", "sequence-other-company");
        otherCompanyRequest.put("companyName", "Branch Company");
        Long thirdId = createAppointment(token, otherCompanyRequest);

        assertAppointmentSequences(token, firstId, 1, 1);
        assertAppointmentSequences(token, secondId, 1, 2);
        assertAppointmentSequences(token, thirdId, 2, 3);

        JsonNode listedBeforeDelete = listAppointments(token);
        assertThat(listedBeforeDelete.path("data").path("items")).hasSize(3);
        assertThat(listedBeforeDelete.path("data").path("items").get(0).path("id").asLong()).isEqualTo(firstId);
        assertThat(listedBeforeDelete.path("data").path("items").get(1).path("id").asLong()).isEqualTo(secondId);
        assertThat(listedBeforeDelete.path("data").path("items").get(2).path("id").asLong()).isEqualTo(thirdId);
        assertThat(listedBeforeDelete.path("data").path("items").get(0).path("globalSequence").asLong()).isEqualTo(1);
        assertThat(listedBeforeDelete.path("data").path("items").get(1).path("globalSequence").asLong()).isEqualTo(1);
        assertThat(listedBeforeDelete.path("data").path("items").get(2).path("globalSequence").asLong()).isEqualTo(2);
        assertThat(listedBeforeDelete.path("data").path("items").get(0).path("displaySequence").asLong()).isEqualTo(1);
        assertThat(listedBeforeDelete.path("data").path("items").get(1).path("displaySequence").asLong()).isEqualTo(2);
        assertThat(listedBeforeDelete.path("data").path("items").get(2).path("displaySequence").asLong()).isEqualTo(3);

        mockMvc.perform(delete("/api/party-hr/appointments/{id}", secondId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        JsonNode listedAfterDelete = listAppointments(token);
        assertThat(listedAfterDelete.path("data").path("items")).hasSize(2);
        assertThat(listedAfterDelete.path("data").path("items").get(0).path("id").asLong()).isEqualTo(firstId);
        assertThat(listedAfterDelete.path("data").path("items").get(1).path("id").asLong()).isEqualTo(thirdId);
        assertThat(listedAfterDelete.path("data").path("items").get(0).path("globalSequence").asLong()).isEqualTo(1);
        assertThat(listedAfterDelete.path("data").path("items").get(1).path("globalSequence").asLong()).isEqualTo(2);
        assertThat(listedAfterDelete.path("data").path("items").get(0).path("displaySequence").asLong()).isEqualTo(1);
        assertThat(listedAfterDelete.path("data").path("items").get(1).path("displaySequence").asLong()).isEqualTo(2);
        assertAppointmentSequences(token, thirdId, 2, 2);

        Long fourthId = createAppointment(token, createRequest("sequence-after-deleted-company-max", "reason", List.of()));
        assertAppointmentSequences(token, fourthId, 1, 3);

        JsonNode listedAfterRecreate = listAppointments(token);
        assertThat(listedAfterRecreate.path("data").path("items")).hasSize(3);
        assertThat(listedAfterRecreate.path("data").path("items").get(0).path("id").asLong()).isEqualTo(firstId);
        assertThat(listedAfterRecreate.path("data").path("items").get(1).path("id").asLong()).isEqualTo(thirdId);
        assertThat(listedAfterRecreate.path("data").path("items").get(2).path("id").asLong()).isEqualTo(fourthId);
        assertThat(listedAfterRecreate.path("data").path("items").get(0).path("globalSequence").asLong()).isEqualTo(1);
        assertThat(listedAfterRecreate.path("data").path("items").get(1).path("globalSequence").asLong()).isEqualTo(2);
        assertThat(listedAfterRecreate.path("data").path("items").get(2).path("globalSequence").asLong()).isEqualTo(1);
        assertThat(listedAfterRecreate.path("data").path("items").get(0).path("displaySequence").asLong()).isEqualTo(1);
        assertThat(listedAfterRecreate.path("data").path("items").get(1).path("displaySequence").asLong()).isEqualTo(2);
        assertThat(listedAfterRecreate.path("data").path("items").get(2).path("displaySequence").asLong()).isEqualTo(3);
    }

    @Test
    void updateCompanyNormalizesGlobalSequenceWithoutChangingDisplaySequence() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> companyARequest = mutableCreateRequest();
        companyARequest.put("name", "company-a-record");
        companyARequest.put("companyName", "Company A");
        Long companyAId = createAppointment(token, companyARequest);

        Map<String, Object> companyBRequest = mutableCreateRequest();
        companyBRequest.put("name", "company-b-record");
        companyBRequest.put("companyName", "Company B");
        Long companyBId = createAppointment(token, companyBRequest);

        assertAppointmentSequences(token, companyAId, 1, 1);
        assertAppointmentSequences(token, companyBId, 2, 2);

        Map<String, Object> movedToCompanyBRequest = mutableCreateRequest();
        movedToCompanyBRequest.put("name", "company-a-moved-to-b");
        movedToCompanyBRequest.put("companyName", "Company B");
        mockMvc.perform(put("/api/party-hr/appointments/{id}", companyAId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movedToCompanyBRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("Company B"))
                .andExpect(jsonPath("$.data.globalSequence").value(1))
                .andExpect(jsonPath("$.data.displaySequence").value(1));

        assertAppointmentSequences(token, companyBId, 1, 2);
        JsonNode listed = listAppointments(token);
        assertThat(listed.path("data").path("items")).hasSize(2);
        assertThat(listed.path("data").path("items").get(0).path("id").asLong()).isEqualTo(companyAId);
        assertThat(listed.path("data").path("items").get(0).path("globalSequence").asLong()).isEqualTo(1);
        assertThat(listed.path("data").path("items").get(0).path("displaySequence").asLong()).isEqualTo(1);
        assertThat(listed.path("data").path("items").get(1).path("id").asLong()).isEqualTo(companyBId);
        assertThat(listed.path("data").path("items").get(1).path("globalSequence").asLong()).isEqualTo(1);
        assertThat(listed.path("data").path("items").get(1).path("displaySequence").asLong()).isEqualTo(2);
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
    void createsAppointmentWhenLegacyHiddenFieldsAreBlank() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.put("positionName", "");
        request.put("graduationSchool", "");
        request.put("address", "");

        Long appointmentId = createAppointment(token, request);

        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.positionName").value(""))
                .andExpect(jsonPath("$.data.graduationSchool").value(""))
                .andExpect(jsonPath("$.data.address").value(""));
    }

    @Test
    void createsAppointmentWhenLegacyHiddenFieldsAreOmitted() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.remove("positionName");
        request.remove("graduationSchool");
        request.remove("address");

        Long appointmentId = createAppointment(token, request);

        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.positionName").value(""))
                .andExpect(jsonPath("$.data.graduationSchool").value(""))
                .andExpect(jsonPath("$.data.address").value(""));
    }

    @Test
    void createsAppointmentWhenIdCardIsBlankOrOmitted() throws Exception {
        String token = loginSuperadmin().token();

        Map<String, Object> blankRequest = mutableCreateRequest();
        blankRequest.put("name", "blank-id-card-user");
        blankRequest.put("idCard", "");
        Long blankAppointmentId = createAppointment(token, blankRequest);

        Map<String, Object> omittedRequest = mutableCreateRequest();
        omittedRequest.put("name", "omitted-id-card-user");
        omittedRequest.remove("idCard");
        Long omittedAppointmentId = createAppointment(token, omittedRequest);

        mockMvc.perform(get("/api/party-hr/appointments/{id}", blankAppointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idCard").value(""));

        mockMvc.perform(get("/api/party-hr/appointments/{id}", omittedAppointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idCard").value(""));

        mockMvc.perform(get("/api/party-hr/appointments")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[*].name", hasItem("blank-id-card-user")))
                .andExpect(jsonPath("$.data.items[*].name", hasItem("omitted-id-card-user")))
                .andExpect(jsonPath("$.data.items[*].companyName", hasItem("集团公司")))
                .andExpect(jsonPath("$.data.items[*].departmentName", hasItem("党群人力部")))
                .andExpect(jsonPath("$.data.items[*].currentPosition", hasItem("党群主管")))
                .andExpect(jsonPath("$.data.items[*].phone", hasItem("13900001111")))
                .andExpect(jsonPath("$.data.items[*].idCard", hasItem("")));
    }

    @Test
    void rejectsMissingOrBlankNewAppointmentRequiredFields() throws Exception {
        String token = loginSuperadmin().token();

        // 新版审批表保存入口的五个必填字段，前后端必须保持同一套校验契约。
        for (String fieldName : List.of("name", "companyName", "departmentName", "currentPosition", "phone")) {
            assertAppointmentRequiredFieldRejected(token, fieldName, null);
            assertAppointmentRequiredFieldRejected(token, fieldName, " ");
        }
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
    void rejectsInvalidAppointmentBoardDepartment() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.put("departmentName", "不存在的部门");

        mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("所属部门")))
                .andExpect(jsonPath("$.message", not(containsString("DataIntegrity"))))
                .andExpect(jsonPath("$.message", not(containsString("constraint"))));
    }

    @Test
    void rejectsMissingAppointmentBoardDepartment() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.remove("departmentName");

        mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("departmentName")))
                .andExpect(jsonPath("$.message", not(containsString("DataIntegrity"))))
                .andExpect(jsonPath("$.message", not(containsString("constraint"))));
    }

    @Test
    void ageIsNullWhenBirthDateIsMissing() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.remove("birthDate");
        Long appointmentId = createAppointment(token, request);

        assertAppointmentAgeIsNull(token, appointmentId);
    }

    @Test
    void ageIsNullWhenBirthDateIsInFuture() throws Exception {
        String token = loginSuperadmin().token();
        Map<String, Object> request = mutableCreateRequest();
        request.put("birthDate", "2026-06-03");
        Long appointmentId = createAppointment(token, request);

        assertAppointmentAgeIsNull(token, appointmentId);
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

    private JsonNode listAppointments(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/party-hr/appointments")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertAppointmentSequences(
            String token,
            Long appointmentId,
            long expectedGlobalSequence,
            long expectedDisplaySequence) throws Exception {
        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.globalSequence").value(expectedGlobalSequence))
                .andExpect(jsonPath("$.data.displaySequence").value(expectedDisplaySequence));
    }

    private void assertAppointmentAgeIsNull(String token, Long appointmentId) throws Exception {
        mockMvc.perform(get("/api/party-hr/appointments")
                        .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(appointmentId.intValue()))
                .andExpect(jsonPath("$.data.items[0].age").value(nullValue()));

        mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(appointmentId))
                .andExpect(jsonPath("$.data.age").value(nullValue()));
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

    private void assertAppointmentRequiredFieldRejected(String token, String fieldName, Object fieldValue) throws Exception {
        Map<String, Object> request = mutableCreateRequest();
        if (fieldValue == null) {
            request.remove(fieldName);
        } else {
            request.put(fieldName, fieldValue);
        }

        mockMvc.perform(post("/api/party-hr/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString(fieldName)))
                .andExpect(jsonPath("$.message", not(containsString("DataIntegrity"))))
                .andExpect(jsonPath("$.message", not(containsString("constraint"))));
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
                Map.entry("companyName", "集团公司"),
                Map.entry("departmentName", "党群人力部"),
                Map.entry("graduationSchool", "中国人民大学"),
                Map.entry("address", "北京市朝阳区"),
                Map.entry("gender", "男"),
                Map.entry("birthDate", "1990-01-01"),
                Map.entry("ethnicity", "汉族"),
                Map.entry("politicalStatus", "中共党员"),
                Map.entry("nativePlace", "山东济南"),
                Map.entry("birthPlace", "北京"),
                Map.entry("partyJoinDate", "2012-07-01"),
                Map.entry("workStartDate", "2013-08-01"),
                Map.entry("healthStatus", "健康"),
                Map.entry("technicalPosition", "高级政工师"),
                Map.entry("specialty", "公共管理"),
                Map.entry("fullTimeEducation", "本科"),
                Map.entry("fullTimeEducationDegree", "学士"),
                Map.entry("fullTimeSchool", "中国人民大学"),
                Map.entry("fullTimeMajor", "行政管理"),
                Map.entry("fullTimeSchoolMajor", "中国人民大学 行政管理"),
                Map.entry("inServiceEducation", "硕士"),
                Map.entry("inServiceSchoolMajor", "中央党校 经济管理"),
                Map.entry("partTimeEducation", "硕士研究生"),
                Map.entry("partTimeDegree", "硕士"),
                Map.entry("partTimeSchool", "中央党校"),
                Map.entry("partTimeMajor", "经济管理"),
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
                Map.entry("maritalStatus", "已婚"),
                Map.entry("remark", "看板备注"),
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

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-06-02T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
        }
    }
}
