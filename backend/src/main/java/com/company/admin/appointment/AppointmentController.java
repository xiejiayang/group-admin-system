package com.company.admin.appointment;

import com.company.admin.appointment.dto.AppointmentDetailResponse;
import com.company.admin.appointment.dto.AppointmentPageResponse;
import com.company.admin.appointment.dto.AppointmentRecordRequest;
import com.company.admin.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/party-hr/appointments")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('appointment:manage')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ApiResponse<AppointmentPageResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(appointmentService.list(authentication.getName(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentDetailResponse> detail(
            Authentication authentication,
            @PathVariable Long id) {
        return ApiResponse.ok(appointmentService.detail(authentication.getName(), id));
    }

    @PostMapping
    public ApiResponse<AppointmentDetailResponse> create(
            Authentication authentication,
            @Valid @RequestBody AppointmentRecordRequest request) {
        return ApiResponse.ok(appointmentService.create(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AppointmentDetailResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRecordRequest request) {
        return ApiResponse.ok(appointmentService.update(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long id) {
        appointmentService.delete(authentication.getName(), id);
        return ApiResponse.ok(null);
    }
}
