package com.company.admin.appointment;

import com.company.admin.appointment.dto.AppointmentDetailResponse;
import com.company.admin.appointment.dto.AppointmentFamilyMemberRequest;
import com.company.admin.appointment.dto.AppointmentFamilyMemberResponse;
import com.company.admin.appointment.dto.AppointmentPageResponse;
import com.company.admin.appointment.dto.AppointmentRecordRequest;
import com.company.admin.appointment.dto.AppointmentSummaryResponse;
import com.company.admin.common.BusinessException;
import com.company.admin.file.FileBusinessTypes;
import com.company.admin.file.SysFileRepository;
import com.company.admin.system.Department;
import com.company.admin.system.DepartmentAccessPolicy;
import com.company.admin.system.Permission;
import com.company.admin.system.Role;
import com.company.admin.system.User;
import com.company.admin.system.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final String PARTY_HR_DEPARTMENT = "PARTY_HR";
    private static final String APPOINTMENT_MANAGE_PERMISSION = "appointment:manage";
    private static final String DEFAULT_APPROVAL_AUTHORITY_OPINION = "此表信息已认定";

    private final AppointmentRecordRepository appointmentRecordRepository;
    private final UserRepository userRepository;
    private final SysFileRepository sysFileRepository;

    public AppointmentService(
            AppointmentRecordRepository appointmentRecordRepository,
            UserRepository userRepository,
            SysFileRepository sysFileRepository) {
        this.appointmentRecordRepository = appointmentRecordRepository;
        this.userRepository = userRepository;
        this.sysFileRepository = sysFileRepository;
    }

    @Transactional(readOnly = true)
    public AppointmentPageResponse list(String username, int page, int size) {
        requirePartyHrAppointmentManager(username);
        PageRequest pageRequest = pageRequest(page, size);
        Page<AppointmentRecord> appointments = appointmentRecordRepository.findByDeletedFalse(pageRequest);
        return new AppointmentPageResponse(
                appointments.getContent().stream()
                        .map(this::toSummaryResponse)
                        .toList(),
                appointments.getTotalElements(),
                appointments.getNumber(),
                appointments.getSize());
    }

    @Transactional(readOnly = true)
    public AppointmentDetailResponse detail(String username, Long id) {
        requirePartyHrAppointmentManager(username);
        AppointmentRecord record = activeRecord(id);
        return toDetailResponse(record);
    }

    @Transactional
    public AppointmentDetailResponse create(String username, AppointmentRecordRequest request) {
        User operator = requirePartyHrAppointmentManager(username);
        AppointmentRecord record = new AppointmentRecord();
        mapEditableFields(record, request);
        record.setCreatedBy(operator.getId());
        record.setUpdatedBy(operator.getId());
        replaceFamilyMembers(record, request.familyMembers());
        return toDetailResponse(appointmentRecordRepository.save(record));
    }

    @Transactional
    public AppointmentDetailResponse update(String username, Long id, AppointmentRecordRequest request) {
        User operator = requirePartyHrAppointmentManager(username);
        AppointmentRecord record = activeRecord(id);
        mapEditableFields(record, request);
        record.setUpdatedBy(operator.getId());
        replaceFamilyMembers(record, request.familyMembers());
        return toDetailResponse(record);
    }

    @Transactional
    public void delete(String username, Long id) {
        User operator = requirePartyHrAppointmentManager(username);
        AppointmentRecord record = activeRecord(id);
        record.setDeleted(true);
        record.setUpdatedBy(operator.getId());
    }

    private User requirePartyHrAppointmentManager(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .filter(this::isEnabled)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "认证失败，请重新登录"));
        if (hasRole(user, DepartmentAccessPolicy.SUPER_ADMIN_ROLE)) {
            return user;
        }

        Set<String> permissionCodes = permissionCodes(user);
        String departmentCode = departmentCode(user);
        // 任免审批接口不仅看权限，还必须确认部门，防止跨部门直接调用。
        if (!permissionCodes.contains(APPOINTMENT_MANAGE_PERMISSION) || !PARTY_HR_DEPARTMENT.equals(departmentCode)) {
            throw new AccessDeniedException("无权访问该资源");
        }
        return user;
    }

    private AppointmentRecord activeRecord(Long id) {
        return appointmentRecordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "任免审批记录不存在"));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new BusinessException("分页参数不合法");
        }
        return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "id"));
    }

    private void mapEditableFields(AppointmentRecord record, AppointmentRecordRequest request) {
        validatePhotoFile(request.photoFileId());

        record.setName(request.name());
        record.setPhone(request.phone());
        record.setIdCard(request.idCard());
        record.setPositionName(request.positionName());
        record.setGraduationSchool(request.graduationSchool());
        record.setAddress(request.address());

        // 以下映射与任免审批 Word 表单字段一一对应，便于后续按原表单版式回填或导出。
        record.setGender(request.gender());
        record.setBirthDate(request.birthDate());
        record.setEthnicity(request.ethnicity());
        record.setNativePlace(request.nativePlace());
        record.setBirthPlace(request.birthPlace());
        record.setPartyJoinDate(request.partyJoinDate());
        record.setWorkStartDate(request.workStartDate());
        record.setHealthStatus(request.healthStatus());
        record.setTechnicalPosition(request.technicalPosition());
        record.setSpecialty(request.specialty());
        record.setFullTimeEducation(request.fullTimeEducation());
        record.setFullTimeSchoolMajor(request.fullTimeSchoolMajor());
        record.setInServiceEducation(request.inServiceEducation());
        record.setInServiceSchoolMajor(request.inServiceSchoolMajor());
        record.setCurrentPosition(request.currentPosition());
        record.setProposedPosition(request.proposedPosition());
        record.setProposedRemovalPosition(request.proposedRemovalPosition());
        record.setResumeText(request.resumeText());
        record.setRewardPunishment(request.rewardPunishment());
        record.setAnnualAssessmentResult(request.annualAssessmentResult());
        record.setAppointmentReason(request.appointmentReason());
        record.setReportingUnit(request.reportingUnit());
        record.setReportingUnitDate(request.reportingUnitDate());
        record.setApprovalAuthorityOpinion(defaultApprovalOpinion(request.approvalAuthorityOpinion()));
        record.setApprovalAuthorityDate(request.approvalAuthorityDate());
        record.setAdministrativeAppointmentOpinion(request.administrativeAppointmentOpinion());
        record.setAdministrativeAppointmentDate(request.administrativeAppointmentDate());
        record.setFormFiller(request.formFiller());
        record.setPhotoFileId(request.photoFileId());
    }

    private void validatePhotoFile(Long photoFileId) {
        if (photoFileId == null) {
            return;
        }
        // 照片文件由上传模块统一管理，任免表只引用已校验文件，避免直接依赖外键暴露数据库异常。
        boolean validPhoto = sysFileRepository.existsByIdAndBusinessTypeAndDeletedFalse(
                photoFileId,
                FileBusinessTypes.ID_PHOTO);
        if (!validPhoto) {
            throw new BusinessException("照片文件不存在或不可用");
        }
    }

    private void replaceFamilyMembers(
            AppointmentRecord record,
            List<AppointmentFamilyMemberRequest> familyMemberRequests) {
        record.getFamilyMembers().clear();
        if (familyMemberRequests == null) {
            return;
        }

        familyMemberRequests.forEach(memberRequest -> {
            AppointmentFamilyMember member = new AppointmentFamilyMember();
            member.setAppointmentRecord(record);
            member.setRelationship(memberRequest.relationship());
            member.setName(memberRequest.name());
            member.setAge(memberRequest.age());
            member.setPoliticalStatus(memberRequest.politicalStatus());
            member.setWorkUnitAndPosition(memberRequest.workUnitAndPosition());
            member.setSortOrder(memberRequest.sortOrder() == null ? 0 : memberRequest.sortOrder());
            record.getFamilyMembers().add(member);
        });
    }

    private String defaultApprovalOpinion(String approvalAuthorityOpinion) {
        if (approvalAuthorityOpinion == null || approvalAuthorityOpinion.isBlank()) {
            return DEFAULT_APPROVAL_AUTHORITY_OPINION;
        }
        return approvalAuthorityOpinion;
    }

    private AppointmentSummaryResponse toSummaryResponse(AppointmentRecord record) {
        return new AppointmentSummaryResponse(
                record.getId(),
                record.getName(),
                record.getPhone(),
                record.getIdCard(),
                record.getPositionName(),
                record.getGraduationSchool(),
                record.getAddress());
    }

    private AppointmentDetailResponse toDetailResponse(AppointmentRecord record) {
        return new AppointmentDetailResponse(
                record.getId(),
                record.getName(),
                record.getPhone(),
                record.getIdCard(),
                record.getPositionName(),
                record.getGraduationSchool(),
                record.getAddress(),
                record.getGender(),
                record.getBirthDate(),
                record.getEthnicity(),
                record.getNativePlace(),
                record.getBirthPlace(),
                record.getPartyJoinDate(),
                record.getWorkStartDate(),
                record.getHealthStatus(),
                record.getTechnicalPosition(),
                record.getSpecialty(),
                record.getFullTimeEducation(),
                record.getFullTimeSchoolMajor(),
                record.getInServiceEducation(),
                record.getInServiceSchoolMajor(),
                record.getCurrentPosition(),
                record.getProposedPosition(),
                record.getProposedRemovalPosition(),
                record.getResumeText(),
                record.getRewardPunishment(),
                record.getAnnualAssessmentResult(),
                record.getAppointmentReason(),
                record.getReportingUnit(),
                record.getReportingUnitDate(),
                record.getApprovalAuthorityOpinion(),
                record.getApprovalAuthorityDate(),
                record.getAdministrativeAppointmentOpinion(),
                record.getAdministrativeAppointmentDate(),
                record.getFormFiller(),
                record.getPhotoFileId(),
                record.getFamilyMembers().stream()
                        .sorted(Comparator.comparingInt(AppointmentFamilyMember::getSortOrder)
                                .thenComparing(member -> member.getId() == null ? 0L : member.getId()))
                        .map(this::toFamilyMemberResponse)
                        .toList());
    }

    private AppointmentFamilyMemberResponse toFamilyMemberResponse(AppointmentFamilyMember member) {
        return new AppointmentFamilyMemberResponse(
                member.getId(),
                member.getRelationship(),
                member.getName(),
                member.getAge(),
                member.getPoliticalStatus(),
                member.getWorkUnitAndPosition(),
                member.getSortOrder());
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream()
                .filter(Role::isEnabled)
                .map(Role::getCode)
                .anyMatch(roleCode::equals);
    }

    private Set<String> permissionCodes(User user) {
        return user.getRoles().stream()
                .filter(Role::isEnabled)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String departmentCode(User user) {
        Department department = user.getDepartment();
        return department == null ? null : department.getCode();
    }

    private boolean isEnabled(User user) {
        return User.STATUS_ENABLED.equals(user.getStatus());
    }
}
