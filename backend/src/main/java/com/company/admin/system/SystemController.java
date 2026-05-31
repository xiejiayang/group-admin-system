package com.company.admin.system;

import com.company.admin.common.ApiResponse;
import com.company.admin.common.BusinessException;
import com.company.admin.system.dto.AssignRolesRequest;
import com.company.admin.system.dto.MenuResponse;
import com.company.admin.system.dto.RoleResponse;
import com.company.admin.system.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuResponse>> menus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录");
        }
        return ApiResponse.ok(systemService.currentUserMenus(authentication.getName()));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<UserResponse>> users() {
        return ApiResponse.ok(systemService.users());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<RoleResponse>> roles() {
        return ApiResponse.ok(systemService.roles());
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.ok(systemService.assignRoles(userId, request));
    }
}
