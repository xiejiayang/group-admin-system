package com.company.admin.appointment.dto;

public record AppointmentSummaryResponse(
        Long id,
        Long globalSequence,
        Long displaySequence,
        String companyName,
        String departmentName,
        String name,
        String currentPosition,
        String gender,
        String ethnicity,
        String idCard,
        Integer age,
        String politicalStatus,
        String fullTimeEducation,
        String fullTimeEducationDegree,
        String fullTimeSchool,
        String fullTimeMajor,
        String partTimeEducation,
        String partTimeDegree,
        String partTimeSchool,
        String partTimeMajor,
        String technicalPosition,
        String phone,
        String maritalStatus,
        String remark) {
}
