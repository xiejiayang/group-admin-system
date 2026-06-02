package com.company.admin.appointment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record AppointmentRecordRequest(
        // 以下必填字段与任免审批表新版保存入口保持一致。
        @NotBlank(message = "不能为空")
        @Size(max = 64)
        String name,

        @NotBlank(message = "不能为空")
        @Size(max = 32)
        String phone,

        @NotBlank(message = "不能为空")
        @Size(max = 32)
        String idCard,

        @Size(max = 100)
        String positionName,

        @NotBlank(message = "不能为空")
        @Size(max = 120)
        String companyName,

        @NotBlank(message = "不能为空")
        @Size(max = 120)
        String departmentName,

        @Size(max = 160)
        String graduationSchool,

        @Size(max = 255)
        String address,

        @Size(max = 20)
        String gender,
        LocalDate birthDate,
        @Size(max = 60)
        String ethnicity,
        @Size(max = 80)
        String politicalStatus,
        @Size(max = 120)
        String nativePlace,
        @Size(max = 120)
        String birthPlace,
        LocalDate partyJoinDate,
        LocalDate workStartDate,
        @Size(max = 80)
        String healthStatus,
        @Size(max = 120)
        String technicalPosition,
        @Size(max = 255)
        String specialty,
        @Size(max = 160)
        String fullTimeEducation,
        @Size(max = 120)
        String fullTimeEducationDegree,
        @Size(max = 160)
        String fullTimeSchool,
        @Size(max = 160)
        String fullTimeMajor,
        @Size(max = 255)
        String fullTimeSchoolMajor,
        @Size(max = 160)
        String inServiceEducation,
        @Size(max = 255)
        String inServiceSchoolMajor,
        @Size(max = 160)
        String partTimeEducation,
        @Size(max = 120)
        String partTimeDegree,
        @Size(max = 160)
        String partTimeSchool,
        @Size(max = 160)
        String partTimeMajor,
        // 现任职务也是新版任免审批表保存入口的必填字段。
        @NotBlank(message = "不能为空")
        @Size(max = 255)
        String currentPosition,
        @Size(max = 255)
        String proposedPosition,
        @Size(max = 255)
        String proposedRemovalPosition,
        String resumeText,
        String rewardPunishment,
        String annualAssessmentResult,
        String appointmentReason,
        String reportingUnit,
        LocalDate reportingUnitDate,
        String approvalAuthorityOpinion,
        LocalDate approvalAuthorityDate,
        String administrativeAppointmentOpinion,
        LocalDate administrativeAppointmentDate,
        @Size(max = 100)
        String formFiller,
        @Size(max = 40)
        String maritalStatus,
        String remark,
        Long photoFileId,
        @Valid
        List<@Valid AppointmentFamilyMemberRequest> familyMembers) {
}
