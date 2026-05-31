package com.company.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank(message = "不能为空")
        String username,

        @NotBlank(message = "不能为空")
        String password,

        @NotBlank(message = "不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "格式不正确")
        String phone,

        @NotBlank(message = "不能为空")
        String departmentCode) {
}
