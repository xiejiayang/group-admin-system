package com.company.admin.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AppointmentFamilyMemberRequest(
        @Size(max = 64)
        String relationship,
        @Size(max = 64)
        String name,
        @Min(0)
        @Max(150)
        Integer age,
        @Size(max = 80)
        String politicalStatus,
        @Size(max = 255)
        String workUnitAndPosition,
        @PositiveOrZero
        Integer sortOrder) {
}
