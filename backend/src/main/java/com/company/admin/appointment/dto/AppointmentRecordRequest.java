package com.company.admin.appointment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record AppointmentRecordRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 64)
        String name,

        @NotBlank(message = "不能为空")
        @Size(max = 32)
        String phone,

        @NotBlank(message = "不能为空")
        @Size(max = 32)
        String idCard,

        @NotBlank(message = "不能为空")
        @Size(max = 100)
        String positionName,

        @NotBlank(message = "不能为空")
        @Size(max = 160)
        String graduationSchool,

        @NotBlank(message = "不能为空")
        @Size(max = 255)
        String address,

        @Size(max = 20)
        String gender,
        LocalDate birthDate,
        @Size(max = 60)
        String ethnicity,
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
        @Size(max = 255)
        String fullTimeSchoolMajor,
        @Size(max = 160)
        String inServiceEducation,
        @Size(max = 255)
        String inServiceSchoolMajor,
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
        Long photoFileId,
        @Valid
        List<@Valid AppointmentFamilyMemberRequest> familyMembers) {
}
