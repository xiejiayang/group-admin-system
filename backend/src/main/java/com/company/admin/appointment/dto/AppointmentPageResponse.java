package com.company.admin.appointment.dto;

import java.util.List;

public record AppointmentPageResponse(
        List<AppointmentSummaryResponse> items,
        long total,
        int page,
        int size) {
}
