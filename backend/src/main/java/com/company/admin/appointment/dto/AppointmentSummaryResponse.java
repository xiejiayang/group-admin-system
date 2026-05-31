package com.company.admin.appointment.dto;

public record AppointmentSummaryResponse(
        Long id,
        String name,
        String phone,
        String idCard,
        String positionName,
        String graduationSchool,
        String address) {
}
