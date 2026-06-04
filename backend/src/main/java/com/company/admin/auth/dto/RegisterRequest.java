package com.company.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过64个字符")
        String username,

        @NotBlank(message = "不能为空")
        @Size(max = 64, message = "长度不能超过64个字符")
        String realName,

        @NotBlank(message = "不能为空")
        String password,

        @NotBlank(message = "不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "格式不正确")
        String phone,

        @NotBlank(message = "不能为空")
        String departmentCode) {

    public RegisterRequest(String username, String password, String phone, String departmentCode) {
        this(username, username, password, phone, departmentCode);
    }
}
