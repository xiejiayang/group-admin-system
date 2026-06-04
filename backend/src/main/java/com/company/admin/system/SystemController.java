package com.company.admin.system;

import com.company.admin.common.ApiResponse;
import com.company.admin.common.BusinessException;
import com.company.admin.system.dto.AssignRolesRequest;
import com.company.admin.system.dto.MenuResponse;
import com.company.admin.system.dto.OperationLogResponse;
import com.company.admin.system.dto.RoleResponse;
import com.company.admin.system.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return ApiResponse.ok(systemService.currentUserMenus(username(authentication)));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> users(Authentication authentication) {
        return ApiResponse.ok(systemService.users(username(authentication)));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> roles(
            Authentication authentication,
            @RequestParam Long targetUserId) {
        return ApiResponse.ok(systemService.roles(username(authentication), targetUserId));
    }

    @PutMapping("/users/{userId}/roles")
    public ApiResponse<UserResponse> assignRoles(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.ok(systemService.assignRoles(username(authentication), userId, request));
    }

    @GetMapping("/logs")
    public ApiResponse<List<OperationLogResponse>> logs(Authentication authentication) {
        return ApiResponse.ok(systemService.operationLogs(username(authentication)));
    }

    private String username(Authentication authentication) {
        // 控制器只确认请求已认证，具体系统资源授权统一下沉到 SystemService，避免注解角色和业务规则分叉。
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录");
        }
        return authentication.getName();
    }
}
