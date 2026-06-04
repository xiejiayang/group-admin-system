package com.company.admin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignRolesRequest(
        @NotNull
        @NotEmpty(message = "角色不能为空")
        List<@NotBlank String> roleCodes) {
}
