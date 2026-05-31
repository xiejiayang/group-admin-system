package com.company.admin.appointment.dto;

public record AppointmentFamilyMemberRequest(
        String relationship,
        String name,
        Integer age,
        String politicalStatus,
        String workUnitAndPosition,
        Integer sortOrder) {
}
