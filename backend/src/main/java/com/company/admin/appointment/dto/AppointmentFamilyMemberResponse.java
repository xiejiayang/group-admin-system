package com.company.admin.appointment.dto;

public record AppointmentFamilyMemberResponse(
        Long id,
        String relationship,
        String name,
        Integer age,
        String politicalStatus,
        String workUnitAndPosition,
        Integer sortOrder) {
}
