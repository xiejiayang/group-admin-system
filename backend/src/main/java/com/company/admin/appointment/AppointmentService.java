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
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
    private static final String DEFAULT_COMPANY_NAME = "集团公司";
    private static final Set<String> APPOINTMENT_BOARD_DEPARTMENTS = Set.of(
            "领导班子",
            "专家顾问",
            "董事会办公室",
            "财务管理部",
            "纪检监察部",
            "党群人力部",
            "综合管理部",
            "融资管理部",
            "产业发展部",
            "法务风控部",
            "建设管理部");

    private final AppointmentRecordRepository appointmentRecordRepository;
    private final UserRepository userRepository;
    private final SysFileRepository sysFileRepository;
    private final Clock clock;

    public AppointmentService(
            AppointmentRecordRepository appointmentRecordRepository,
            UserRepository userRepository,
            SysFileRepository sysFileRepository,
            Clock clock) {
        this.appointmentRecordRepository = appointmentRecordRepository;
        this.userRepository = userRepository;
        this.sysFileRepository = sysFileRepository;
        this.clock = clock;
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
        assignSequences(record);
        record.setCreatedBy(operator.getId());
        record.setUpdatedBy(operator.getId());
        replaceFamilyMembers(record, request.familyMembers());
        return toDetailResponse(appointmentRecordRepository.save(record));
    }

    @Transactional
    public AppointmentDetailResponse update(String username, Long id, AppointmentRecordRequest request) {
        User operator = requirePartyHrAppointmentManager(username);
        AppointmentRecord record = activeRecord(id);
        String originalCompanyName = record.getCompanyName();
        mapEditableFields(record, request);
        reassignGlobalSequenceIfCompanyChanged(record, originalCompanyName);
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
        reorderDisplaySequences();
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
        return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "displaySequence")
                .and(Sort.by(Sort.Direction.ASC, "id")));
    }

    private void assignSequences(AppointmentRecord record) {
        // 总序号按公司独立递增；序号是党群人力部任免看板的整体展示顺序。
        record.setGlobalSequence(appointmentRecordRepository.maxGlobalSequenceByCompanyName(record.getCompanyName()) + 1);
        record.setDisplaySequence(appointmentRecordRepository.maxDisplaySequence() + 1);
    }

    private void reassignGlobalSequenceIfCompanyChanged(AppointmentRecord record, String originalCompanyName) {
        if (Objects.equals(originalCompanyName, record.getCompanyName())) {
            return;
        }
        // 所属公司变更时总序号重新按目标公司续号，展示序号不变。
        record.setGlobalSequence(appointmentRecordRepository.maxGlobalSequenceByCompanyNameAndIdNot(
                record.getCompanyName(),
                record.getId()) + 1);
    }

    private void reorderDisplaySequences() {
        List<AppointmentRecord> records = appointmentRecordRepository.findByDeletedFalseOrderByDisplaySequenceAscIdAsc();
        for (int index = 0; index < records.size(); index++) {
            records.get(index).setDisplaySequence(index + 1L);
        }
    }

    private void mapEditableFields(AppointmentRecord record, AppointmentRecordRequest request) {
        validatePhotoFile(request.photoFileId());

        // 看板所属公司可不填，后端统一落默认值，避免前端遗漏导致非空列写入失败。
        record.setCompanyName(defaultCompanyName(request.companyName()));
        // 所属部门是看板分组字段，只允许预置单选项，避免写入不可展示的自由文本。
        record.setDepartmentName(validateDepartmentName(request.departmentName()));

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
        record.setPoliticalStatus(request.politicalStatus());
        record.setNativePlace(request.nativePlace());
        record.setBirthPlace(request.birthPlace());
        record.setPartyJoinDate(request.partyJoinDate());
        record.setWorkStartDate(request.workStartDate());
        record.setHealthStatus(request.healthStatus());
        record.setTechnicalPosition(request.technicalPosition());
        record.setSpecialty(request.specialty());
        record.setFullTimeEducation(request.fullTimeEducation());
        record.setFullTimeEducationDegree(request.fullTimeEducationDegree());
        record.setFullTimeSchool(request.fullTimeSchool());
        record.setFullTimeMajor(request.fullTimeMajor());
        record.setFullTimeSchoolMajor(request.fullTimeSchoolMajor());
        record.setInServiceEducation(request.inServiceEducation());
        record.setInServiceSchoolMajor(request.inServiceSchoolMajor());
        record.setPartTimeEducation(request.partTimeEducation());
        record.setPartTimeDegree(request.partTimeDegree());
        record.setPartTimeSchool(request.partTimeSchool());
        record.setPartTimeMajor(request.partTimeMajor());
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
        record.setMaritalStatus(request.maritalStatus());
        record.setRemark(request.remark());
        record.setPhotoFileId(request.photoFileId());
    }

    private String defaultCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return DEFAULT_COMPANY_NAME;
        }
        return companyName.trim();
    }

    private String validateDepartmentName(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            throw new BusinessException("所属部门不能为空");
        }
        String normalizedDepartmentName = departmentName.trim();
        if (!APPOINTMENT_BOARD_DEPARTMENTS.contains(normalizedDepartmentName)) {
            throw new BusinessException("所属部门必须选择预置部门");
        }
        return normalizedDepartmentName;
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
                record.getGlobalSequence(),
                record.getDisplaySequence(),
                record.getCompanyName(),
                record.getDepartmentName(),
                record.getName(),
                record.getCurrentPosition(),
                record.getGender(),
                record.getEthnicity(),
                record.getIdCard(),
                calculateAge(record.getBirthDate()),
                record.getPoliticalStatus(),
                record.getFullTimeEducation(),
                record.getFullTimeEducationDegree(),
                record.getFullTimeSchool(),
                record.getFullTimeMajor(),
                record.getPartTimeEducation(),
                record.getPartTimeDegree(),
                record.getPartTimeSchool(),
                record.getPartTimeMajor(),
                record.getTechnicalPosition(),
                record.getPhone(),
                record.getMaritalStatus(),
                record.getRemark());
    }

    private AppointmentDetailResponse toDetailResponse(AppointmentRecord record) {
        // 详情保留旧任免表字段，同时补充看板拆分字段，便于旧审批页和新看板并行演进。
        return new AppointmentDetailResponse(
                record.getId(),
                record.getGlobalSequence(),
                record.getDisplaySequence(),
                record.getCompanyName(),
                record.getDepartmentName(),
                record.getName(),
                record.getPhone(),
                record.getIdCard(),
                record.getPositionName(),
                record.getGraduationSchool(),
                record.getAddress(),
                record.getGender(),
                record.getBirthDate(),
                calculateAge(record.getBirthDate()),
                record.getEthnicity(),
                record.getPoliticalStatus(),
                record.getNativePlace(),
                record.getBirthPlace(),
                record.getPartyJoinDate(),
                record.getWorkStartDate(),
                record.getHealthStatus(),
                record.getTechnicalPosition(),
                record.getSpecialty(),
                record.getFullTimeEducation(),
                record.getFullTimeEducationDegree(),
                record.getFullTimeSchool(),
                record.getFullTimeMajor(),
                record.getFullTimeSchoolMajor(),
                record.getInServiceEducation(),
                record.getInServiceSchoolMajor(),
                record.getPartTimeEducation(),
                record.getPartTimeDegree(),
                record.getPartTimeSchool(),
                record.getPartTimeMajor(),
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
                record.getMaritalStatus(),
                record.getRemark(),
                record.getPhotoFileId(),
                record.getFamilyMembers().stream()
                        .sorted(Comparator.comparingInt(AppointmentFamilyMember::getSortOrder)
                                .thenComparing(member -> member.getId() == null ? 0L : member.getId()))
                        .map(this::toFamilyMemberResponse)
                        .toList());
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        LocalDate today = LocalDate.now(clock);
        // 年龄仅按出生日期即时计算，不落库；未来出生日期视为无效数据并返回空值。
        if (birthDate.isAfter(today)) {
            return null;
        }
        return Period.between(birthDate, today).getYears();
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
