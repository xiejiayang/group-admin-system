package com.company.admin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignRolesRequest(
        @NotNull
        List<@NotBlank String> roleCodes) {
}
