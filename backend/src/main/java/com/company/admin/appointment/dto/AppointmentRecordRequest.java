package com.company.admin.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

public record AppointmentRecordRequest(
        @NotBlank(message = "不能为空")
        String name,

        @NotBlank(message = "不能为空")
        String phone,

        @NotBlank(message = "不能为空")
        String idCard,

        @NotBlank(message = "不能为空")
        String positionName,

        @NotBlank(message = "不能为空")
        String graduationSchool,

        @NotBlank(message = "不能为空")
        String address,

        String gender,
        LocalDate birthDate,
        String ethnicity,
        String nativePlace,
        String birthPlace,
        LocalDate partyJoinDate,
        LocalDate workStartDate,
        String healthStatus,
        String technicalPosition,
        String specialty,
        String fullTimeEducation,
        String fullTimeSchoolMajor,
        String inServiceEducation,
        String inServiceSchoolMajor,
        String currentPosition,
        String proposedPosition,
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
        String formFiller,
        Long photoFileId,
        List<AppointmentFamilyMemberRequest> familyMembers) {
}
