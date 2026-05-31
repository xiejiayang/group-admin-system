package com.company.admin.appointment.dto;

import java.time.LocalDate;
import java.util.List;

public record AppointmentDetailResponse(
        Long id,
        String name,
        String phone,
        String idCard,
        String positionName,
        String graduationSchool,
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
        List<AppointmentFamilyMemberResponse> familyMembers) {
}
