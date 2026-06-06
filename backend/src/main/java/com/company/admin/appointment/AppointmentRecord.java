package com.company.admin.appointment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointment_record")
public class AppointmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_sequence", nullable = false)
    private Long globalSequence = 0L;

    @Column(name = "display_sequence", nullable = false)
    private Long displaySequence = 0L;

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;

    @Column(name = "department_name", nullable = false, length = 120)
    private String departmentName;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(name = "id_card", nullable = false, length = 32)
    private String idCard;

    @Column(name = "position_name", nullable = false, length = 100)
    private String positionName;

    @Column(name = "graduation_school", nullable = false, length = 160)
    private String graduationSchool;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 20)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column
    private Integer age;

    @Column(length = 60)
    private String ethnicity;

    @Column(name = "political_status", length = 80)
    private String politicalStatus;

    @Column(name = "native_place", length = 120)
    private String nativePlace;

    @Column(name = "birth_place", length = 120)
    private String birthPlace;

    @Column(name = "party_join_date")
    private LocalDate partyJoinDate;

    @Column(name = "work_start_date")
    private LocalDate workStartDate;

    @Column(name = "health_status", length = 80)
    private String healthStatus;

    @Column(name = "technical_position", length = 120)
    private String technicalPosition;

    @Column(length = 255)
    private String specialty;

    @Column(name = "full_time_education", length = 160)
    private String fullTimeEducation;

    @Column(name = "full_time_education_degree", length = 120)
    private String fullTimeEducationDegree;

    @Column(name = "full_time_school", length = 160)
    private String fullTimeSchool;

    @Column(name = "full_time_major", length = 160)
    private String fullTimeMajor;

    @Column(name = "full_time_school_major", length = 255)
    private String fullTimeSchoolMajor;

    @Column(name = "in_service_education", length = 160)
    private String inServiceEducation;

    @Column(name = "in_service_school_major", length = 255)
    private String inServiceSchoolMajor;

    @Column(name = "part_time_education", length = 160)
    private String partTimeEducation;

    @Column(name = "part_time_degree", length = 120)
    private String partTimeDegree;

    @Column(name = "part_time_school", length = 160)
    private String partTimeSchool;

    @Column(name = "part_time_major", length = 160)
    private String partTimeMajor;

    @Column(name = "current_position", length = 255)
    private String currentPosition;

    @Column(name = "proposed_position", length = 255)
    private String proposedPosition;

    @Column(name = "proposed_removal_position", length = 255)
    private String proposedRemovalPosition;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;

    @Column(name = "reward_punishment", columnDefinition = "TEXT")
    private String rewardPunishment;

    @Column(name = "annual_assessment_result", columnDefinition = "TEXT")
    private String annualAssessmentResult;

    @Column(name = "appointment_reason", columnDefinition = "TEXT")
    private String appointmentReason;

    @Column(name = "reporting_unit", columnDefinition = "TEXT")
    private String reportingUnit;

    @Column(name = "reporting_unit_date")
    private LocalDate reportingUnitDate;

    @Column(name = "approval_authority_opinion", columnDefinition = "TEXT")
    private String approvalAuthorityOpinion;

    @Column(name = "approval_authority_date")
    private LocalDate approvalAuthorityDate;

    @Column(name = "administrative_appointment_opinion", columnDefinition = "TEXT")
    private String administrativeAppointmentOpinion;

    @Column(name = "administrative_appointment_date")
    private LocalDate administrativeAppointmentDate;

    @Column(name = "form_filler", length = 100)
    private String formFiller;

    @Column(name = "marital_status", length = 40)
    private String maritalStatus;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "photo_file_id")
    private Long photoFileId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "appointmentRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<AppointmentFamilyMember> familyMembers = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Long getGlobalSequence() {
        return globalSequence;
    }

    public void setGlobalSequence(Long globalSequence) {
        this.globalSequence = globalSequence;
    }

    public Long getDisplaySequence() {
        return displaySequence;
    }

    public void setDisplaySequence(Long displaySequence) {
        this.displaySequence = displaySequence;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getGraduationSchool() {
        return graduationSchool;
    }

    public void setGraduationSchool(String graduationSchool) {
        this.graduationSchool = graduationSchool;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public String getPoliticalStatus() {
        return politicalStatus;
    }

    public void setPoliticalStatus(String politicalStatus) {
        this.politicalStatus = politicalStatus;
    }

    public String getNativePlace() {
        return nativePlace;
    }

    public void setNativePlace(String nativePlace) {
        this.nativePlace = nativePlace;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public LocalDate getPartyJoinDate() {
        return partyJoinDate;
    }

    public void setPartyJoinDate(LocalDate partyJoinDate) {
        this.partyJoinDate = partyJoinDate;
    }

    public LocalDate getWorkStartDate() {
        return workStartDate;
    }

    public void setWorkStartDate(LocalDate workStartDate) {
        this.workStartDate = workStartDate;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getTechnicalPosition() {
        return technicalPosition;
    }

    public void setTechnicalPosition(String technicalPosition) {
        this.technicalPosition = technicalPosition;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getFullTimeEducation() {
        return fullTimeEducation;
    }

    public void setFullTimeEducation(String fullTimeEducation) {
        this.fullTimeEducation = fullTimeEducation;
    }

    public String getFullTimeEducationDegree() {
        return fullTimeEducationDegree;
    }

    public void setFullTimeEducationDegree(String fullTimeEducationDegree) {
        this.fullTimeEducationDegree = fullTimeEducationDegree;
    }

    public String getFullTimeSchool() {
        return fullTimeSchool;
    }

    public void setFullTimeSchool(String fullTimeSchool) {
        this.fullTimeSchool = fullTimeSchool;
    }

    public String getFullTimeMajor() {
        return fullTimeMajor;
    }

    public void setFullTimeMajor(String fullTimeMajor) {
        this.fullTimeMajor = fullTimeMajor;
    }

    public String getFullTimeSchoolMajor() {
        return fullTimeSchoolMajor;
    }

    public void setFullTimeSchoolMajor(String fullTimeSchoolMajor) {
        this.fullTimeSchoolMajor = fullTimeSchoolMajor;
    }

    public String getInServiceEducation() {
        return inServiceEducation;
    }

    public void setInServiceEducation(String inServiceEducation) {
        this.inServiceEducation = inServiceEducation;
    }

    public String getInServiceSchoolMajor() {
        return inServiceSchoolMajor;
    }

    public void setInServiceSchoolMajor(String inServiceSchoolMajor) {
        this.inServiceSchoolMajor = inServiceSchoolMajor;
    }

    public String getPartTimeEducation() {
        return partTimeEducation;
    }

    public void setPartTimeEducation(String partTimeEducation) {
        this.partTimeEducation = partTimeEducation;
    }

    public String getPartTimeDegree() {
        return partTimeDegree;
    }

    public void setPartTimeDegree(String partTimeDegree) {
        this.partTimeDegree = partTimeDegree;
    }

    public String getPartTimeSchool() {
        return partTimeSchool;
    }

    public void setPartTimeSchool(String partTimeSchool) {
        this.partTimeSchool = partTimeSchool;
    }

    public String getPartTimeMajor() {
        return partTimeMajor;
    }

    public void setPartTimeMajor(String partTimeMajor) {
        this.partTimeMajor = partTimeMajor;
    }

    public String getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(String currentPosition) {
        this.currentPosition = currentPosition;
    }

    public String getProposedPosition() {
        return proposedPosition;
    }

    public void setProposedPosition(String proposedPosition) {
        this.proposedPosition = proposedPosition;
    }

    public String getProposedRemovalPosition() {
        return proposedRemovalPosition;
    }

    public void setProposedRemovalPosition(String proposedRemovalPosition) {
        this.proposedRemovalPosition = proposedRemovalPosition;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public String getRewardPunishment() {
        return rewardPunishment;
    }

    public void setRewardPunishment(String rewardPunishment) {
        this.rewardPunishment = rewardPunishment;
    }

    public String getAnnualAssessmentResult() {
        return annualAssessmentResult;
    }

    public void setAnnualAssessmentResult(String annualAssessmentResult) {
        this.annualAssessmentResult = annualAssessmentResult;
    }

    public String getAppointmentReason() {
        return appointmentReason;
    }

    public void setAppointmentReason(String appointmentReason) {
        this.appointmentReason = appointmentReason;
    }

    public String getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(String reportingUnit) {
        this.reportingUnit = reportingUnit;
    }

    public LocalDate getReportingUnitDate() {
        return reportingUnitDate;
    }

    public void setReportingUnitDate(LocalDate reportingUnitDate) {
        this.reportingUnitDate = reportingUnitDate;
    }

    public String getApprovalAuthorityOpinion() {
        return approvalAuthorityOpinion;
    }

    public void setApprovalAuthorityOpinion(String approvalAuthorityOpinion) {
        this.approvalAuthorityOpinion = approvalAuthorityOpinion;
    }

    public LocalDate getApprovalAuthorityDate() {
        return approvalAuthorityDate;
    }

    public void setApprovalAuthorityDate(LocalDate approvalAuthorityDate) {
        this.approvalAuthorityDate = approvalAuthorityDate;
    }

    public String getAdministrativeAppointmentOpinion() {
        return administrativeAppointmentOpinion;
    }

    public void setAdministrativeAppointmentOpinion(String administrativeAppointmentOpinion) {
        this.administrativeAppointmentOpinion = administrativeAppointmentOpinion;
    }

    public LocalDate getAdministrativeAppointmentDate() {
        return administrativeAppointmentDate;
    }

    public void setAdministrativeAppointmentDate(LocalDate administrativeAppointmentDate) {
        this.administrativeAppointmentDate = administrativeAppointmentDate;
    }

    public String getFormFiller() {
        return formFiller;
    }

    public void setFormFiller(String formFiller) {
        this.formFiller = formFiller;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getPhotoFileId() {
        return photoFileId;
    }

    public void setPhotoFileId(Long photoFileId) {
        this.photoFileId = photoFileId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<AppointmentFamilyMember> getFamilyMembers() {
        return familyMembers;
    }
}
