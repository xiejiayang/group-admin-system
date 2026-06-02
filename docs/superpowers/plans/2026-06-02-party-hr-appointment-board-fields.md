# 党群人力部任免看板字段升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将党群人力部“任免看板”升级为完整字段版，并让任免审批表弹窗、后端接口、数据库和测试同步支持新字段。

**Architecture:** 后端以 `appointment_record` 为主表新增看板字段，通过 Flyway 迁移兼容旧数据；服务层集中处理序号生成、删除重排、部门校验和年龄计算。前端继续让 `PartyHrView.vue` 负责页面状态，字段展示由 `AppointmentTable.vue` 承担，弹窗字段由 `AppointmentFormGrid.vue` 和类型归一化逻辑承担。

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Flyway, MySQL/H2, Vue 3, TypeScript, Element Plus, Vitest, Playwright。

---

## File Structure

- Create `backend/src/main/resources/db/migration/V2__appointment_board_fields.sql`：新增字段、旧数据回填、初始化序号。
- Modify `backend/src/main/java/com/company/admin/appointment/AppointmentRecord.java`：新增实体字段、getter/setter。
- Modify `backend/src/main/java/com/company/admin/appointment/AppointmentRecordRepository.java`：新增序号查询与重排所需查询方法。
- Modify `backend/src/main/java/com/company/admin/appointment/dto/AppointmentRecordRequest.java`：新增保存请求字段。
- Modify `backend/src/main/java/com/company/admin/appointment/dto/AppointmentDetailResponse.java`：新增详情响应字段。
- Modify `backend/src/main/java/com/company/admin/appointment/dto/AppointmentSummaryResponse.java`：新增看板摘要字段。
- Modify `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`：新增字段映射、部门校验、序号生成、删除重排、年龄计算和中文业务注释。
- Modify `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`：覆盖新增字段、序号生成、删除重排、部门校验。
- Modify `backend/src/test/java/com/company/admin/MigrationSmokeTest.java`：覆盖新增列和迁移约束。
- Modify `frontend/src/types/appointment.ts`：扩展类型、默认值、部门选项、归一化和清洗。
- Modify `frontend/src/components/appointment/AppointmentFormGrid.vue`：新增组织字段、政治面貌、婚姻状况、备注、8 个教育字段。
- Modify `frontend/src/components/appointment/AppointmentFormDialog.vue`：更新必填校验文案和字段。
- Modify `frontend/src/components/appointment/AppointmentTable.vue`：实现新表头和字段列。
- Modify `frontend/src/components/appointment/AppointmentFormGrid.spec.ts`：覆盖新弹窗字段和部门单选。
- Create `frontend/src/types/appointment.spec.ts`：覆盖默认值与清洗逻辑。
- Create `frontend/src/components/appointment/AppointmentTable.spec.ts`：覆盖新表头渲染。
- Modify `frontend/e2e/group-admin.spec.ts`：更新看板表头断言。

---

### Task 1: 后端数据库迁移和迁移测试

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__appointment_board_fields.sql`
- Modify: `backend/src/test/java/com/company/admin/MigrationSmokeTest.java`

- [ ] **Step 1: Write the failing migration smoke test**

Add this test method to `backend/src/test/java/com/company/admin/MigrationSmokeTest.java`:

```java
@Test
void appointmentRecordHasPartyHrBoardColumns() {
    assertThat(columns("appointment_record"))
            .contains(
                    "global_sequence",
                    "display_sequence",
                    "company_name",
                    "department_name",
                    "political_status",
                    "marital_status",
                    "remark",
                    "full_time_education_degree",
                    "full_time_school",
                    "full_time_major",
                    "part_time_education",
                    "part_time_degree",
                    "part_time_school",
                    "part_time_major");
}
```

- [ ] **Step 2: Run the migration test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\AIAPP\.jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -Dtest=MigrationSmokeTest test
```

Expected: FAIL because `appointment_record` does not yet contain the new columns.

- [ ] **Step 3: Add the Flyway migration**

Create `backend/src/main/resources/db/migration/V2__appointment_board_fields.sql`:

```sql
ALTER TABLE appointment_record
  ADD COLUMN global_sequence BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN display_sequence BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN company_name VARCHAR(120) NOT NULL DEFAULT '集团公司',
  ADD COLUMN department_name VARCHAR(120) NOT NULL DEFAULT '党群人力部',
  ADD COLUMN political_status VARCHAR(80) NULL,
  ADD COLUMN marital_status VARCHAR(40) NULL,
  ADD COLUMN remark TEXT NULL,
  ADD COLUMN full_time_education_degree VARCHAR(120) NULL,
  ADD COLUMN full_time_school VARCHAR(160) NULL,
  ADD COLUMN full_time_major VARCHAR(160) NULL,
  ADD COLUMN part_time_education VARCHAR(160) NULL,
  ADD COLUMN part_time_degree VARCHAR(120) NULL,
  ADD COLUMN part_time_school VARCHAR(160) NULL,
  ADD COLUMN part_time_major VARCHAR(160) NULL;

UPDATE appointment_record
SET current_position = position_name
WHERE (current_position IS NULL OR current_position = '')
  AND position_name IS NOT NULL
  AND position_name <> '';

UPDATE appointment_record
SET full_time_school = graduation_school
WHERE (full_time_school IS NULL OR full_time_school = '')
  AND graduation_school IS NOT NULL
  AND graduation_school <> '';

UPDATE appointment_record
SET global_sequence = id
WHERE global_sequence IS NULL;

UPDATE appointment_record
SET display_sequence = id
WHERE display_sequence = 0;

CREATE INDEX idx_appointment_record_company_sequence ON appointment_record(company_name, global_sequence);
CREATE INDEX idx_appointment_record_display_sequence ON appointment_record(display_sequence);
```

- [ ] **Step 4: Run the migration test to verify it passes**

Run:

```powershell
$env:JAVA_HOME='D:\AIAPP\.jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -Dtest=MigrationSmokeTest test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 1**

```powershell
git add backend/src/main/resources/db/migration/V2__appointment_board_fields.sql backend/src/test/java/com/company/admin/MigrationSmokeTest.java
git commit -m "feat: add appointment board field migration"
```

---

### Task 2: 后端 DTO、实体和服务字段映射

**Files:**
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentRecord.java`
- Modify: `backend/src/main/java/com/company/admin/appointment/dto/AppointmentRecordRequest.java`
- Modify: `backend/src/main/java/com/company/admin/appointment/dto/AppointmentDetailResponse.java`
- Modify: `backend/src/main/java/com/company/admin/appointment/dto/AppointmentSummaryResponse.java`
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`
- Modify: `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`

- [ ] **Step 1: Write the failing controller test for new fields**

In `AppointmentControllerTest.superadminCanCreateListReadUpdateAndDeleteAppointmentRecord`, update list assertions to expect new summary fields:

```java
.andExpect(jsonPath("$.data.items[*].globalSequence", hasItem(1)))
.andExpect(jsonPath("$.data.items[*].displaySequence", hasItem(1)))
.andExpect(jsonPath("$.data.items[*].companyName", hasItem("集团公司")))
.andExpect(jsonPath("$.data.items[*].departmentName", hasItem("党群人力部")))
.andExpect(jsonPath("$.data.items[*].currentPosition", hasItem("党群主管")))
.andExpect(jsonPath("$.data.items[*].gender", hasItem("男")))
.andExpect(jsonPath("$.data.items[*].ethnicity", hasItem("汉族")))
.andExpect(jsonPath("$.data.items[*].age", hasItem(36)))
.andExpect(jsonPath("$.data.items[*].politicalStatus", hasItem("中共党员")))
.andExpect(jsonPath("$.data.items[*].fullTimeEducation", hasItem("本科")))
.andExpect(jsonPath("$.data.items[*].fullTimeEducationDegree", hasItem("学士")))
.andExpect(jsonPath("$.data.items[*].fullTimeSchool", hasItem("中国人民大学")))
.andExpect(jsonPath("$.data.items[*].fullTimeMajor", hasItem("行政管理")))
.andExpect(jsonPath("$.data.items[*].partTimeEducation", hasItem("硕士研究生")))
.andExpect(jsonPath("$.data.items[*].partTimeDegree", hasItem("硕士")))
.andExpect(jsonPath("$.data.items[*].partTimeSchool", hasItem("中央党校")))
.andExpect(jsonPath("$.data.items[*].partTimeMajor", hasItem("经济管理")))
.andExpect(jsonPath("$.data.items[*].technicalPosition", hasItem("高级政工师")))
.andExpect(jsonPath("$.data.items[*].phone", hasItem("13900001111")))
.andExpect(jsonPath("$.data.items[*].maritalStatus", hasItem("已婚")))
.andExpect(jsonPath("$.data.items[*].remark", hasItem("备注信息")))
```

Also add detail assertions:

```java
.andExpect(jsonPath("$.data.companyName").value("集团公司"))
.andExpect(jsonPath("$.data.departmentName").value("党群人力部"))
.andExpect(jsonPath("$.data.politicalStatus").value("中共党员"))
.andExpect(jsonPath("$.data.fullTimeEducationDegree").value("学士"))
.andExpect(jsonPath("$.data.fullTimeSchool").value("中国人民大学"))
.andExpect(jsonPath("$.data.fullTimeMajor").value("行政管理"))
.andExpect(jsonPath("$.data.partTimeEducation").value("硕士研究生"))
.andExpect(jsonPath("$.data.partTimeDegree").value("硕士"))
.andExpect(jsonPath("$.data.partTimeSchool").value("中央党校"))
.andExpect(jsonPath("$.data.partTimeMajor").value("经济管理"))
.andExpect(jsonPath("$.data.maritalStatus").value("已婚"))
.andExpect(jsonPath("$.data.remark").value("备注信息"))
```

Update `createRequest` to include:

```java
Map.entry("companyName", "集团公司"),
Map.entry("departmentName", "党群人力部"),
Map.entry("politicalStatus", "中共党员"),
Map.entry("fullTimeEducationDegree", "学士"),
Map.entry("fullTimeSchool", "中国人民大学"),
Map.entry("fullTimeMajor", "行政管理"),
Map.entry("partTimeEducation", "硕士研究生"),
Map.entry("partTimeDegree", "硕士"),
Map.entry("partTimeSchool", "中央党校"),
Map.entry("partTimeMajor", "经济管理"),
Map.entry("maritalStatus", "已婚"),
Map.entry("remark", "备注信息"),
```

- [ ] **Step 2: Run the controller test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\AIAPP\.jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -Dtest=AppointmentControllerTest#superadminCanCreateListReadUpdateAndDeleteAppointmentRecord test
```

Expected: FAIL because request/response DTOs and service mapping do not contain the new fields.

- [ ] **Step 3: Expand backend request and response records**

Update `AppointmentRecordRequest` with these fields after `address`:

```java
@Size(max = 120)
String companyName,
@Size(max = 120)
String departmentName,
@Size(max = 80)
String politicalStatus,
@Size(max = 120)
String fullTimeEducationDegree,
@Size(max = 160)
String fullTimeSchool,
@Size(max = 160)
String fullTimeMajor,
@Size(max = 160)
String partTimeEducation,
@Size(max = 120)
String partTimeDegree,
@Size(max = 160)
String partTimeSchool,
@Size(max = 160)
String partTimeMajor,
@Size(max = 40)
String maritalStatus,
String remark,
```

Update `AppointmentDetailResponse` to include the same editable fields plus `globalSequence`, `displaySequence`, and `age` near the top:

```java
Long globalSequence,
Long displaySequence,
String companyName,
String departmentName,
Integer age,
String politicalStatus,
String fullTimeEducationDegree,
String fullTimeSchool,
String fullTimeMajor,
String partTimeEducation,
String partTimeDegree,
String partTimeSchool,
String partTimeMajor,
String maritalStatus,
String remark,
```

Replace `AppointmentSummaryResponse` with:

```java
public record AppointmentSummaryResponse(
        Long id,
        Long globalSequence,
        Long displaySequence,
        String companyName,
        String departmentName,
        String name,
        String currentPosition,
        String gender,
        String ethnicity,
        String idCard,
        Integer age,
        String politicalStatus,
        String fullTimeEducation,
        String fullTimeEducationDegree,
        String fullTimeSchool,
        String fullTimeMajor,
        String partTimeEducation,
        String partTimeDegree,
        String partTimeSchool,
        String partTimeMajor,
        String technicalPosition,
        String phone,
        String maritalStatus,
        String remark) {
}
```

- [ ] **Step 4: Expand `AppointmentRecord`**

Add fields and accessors in `AppointmentRecord.java`:

```java
@Column(name = "global_sequence", nullable = false)
private Long globalSequence;

@Column(name = "display_sequence", nullable = false)
private Long displaySequence;

@Column(name = "company_name", nullable = false, length = 120)
private String companyName;

@Column(name = "department_name", nullable = false, length = 120)
private String departmentName;

@Column(name = "political_status", length = 80)
private String politicalStatus;

@Column(name = "full_time_education_degree", length = 120)
private String fullTimeEducationDegree;

@Column(name = "full_time_school", length = 160)
private String fullTimeSchool;

@Column(name = "full_time_major", length = 160)
private String fullTimeMajor;

@Column(name = "part_time_education", length = 160)
private String partTimeEducation;

@Column(name = "part_time_degree", length = 120)
private String partTimeDegree;

@Column(name = "part_time_school", length = 160)
private String partTimeSchool;

@Column(name = "part_time_major", length = 160)
private String partTimeMajor;

@Column(name = "marital_status", length = 40)
private String maritalStatus;

@Column(columnDefinition = "TEXT")
private String remark;
```

Add these accessor methods:

```java
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

public String getPoliticalStatus() {
    return politicalStatus;
}

public void setPoliticalStatus(String politicalStatus) {
    this.politicalStatus = politicalStatus;
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
```

- [ ] **Step 5: Map new fields in `AppointmentService`**

Add constants:

```java
private static final String DEFAULT_COMPANY_NAME = "集团公司";
private static final Set<String> PARTY_HR_BOARD_DEPARTMENTS = Set.of(
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
```

In `mapEditableFields`, add this block after base identity fields:

```java
String companyName = defaultCompanyName(request.companyName());
validateBoardDepartment(request.departmentName());

record.setCompanyName(companyName);
record.setDepartmentName(request.departmentName());
record.setPoliticalStatus(request.politicalStatus());
record.setFullTimeEducationDegree(request.fullTimeEducationDegree());
record.setFullTimeSchool(request.fullTimeSchool());
record.setFullTimeMajor(request.fullTimeMajor());
record.setPartTimeEducation(request.partTimeEducation());
record.setPartTimeDegree(request.partTimeDegree());
record.setPartTimeSchool(request.partTimeSchool());
record.setPartTimeMajor(request.partTimeMajor());
record.setMaritalStatus(request.maritalStatus());
record.setRemark(request.remark());
```

Add helpers:

```java
private String defaultCompanyName(String companyName) {
    if (companyName == null || companyName.isBlank()) {
        return DEFAULT_COMPANY_NAME;
    }
    return companyName.trim();
}

private void validateBoardDepartment(String departmentName) {
    if (departmentName == null || departmentName.isBlank()) {
        throw new BusinessException("所属部门不能为空");
    }
    if (!PARTY_HR_BOARD_DEPARTMENTS.contains(departmentName)) {
        throw new BusinessException("所属部门不在任免看板候选范围内");
    }
}
```

Add age helper:

```java
private Integer age(LocalDate birthDate) {
    if (birthDate == null) {
        return null;
    }
    LocalDate today = LocalDate.now();
    if (birthDate.isAfter(today)) {
        return null;
    }
    return java.time.Period.between(birthDate, today).getYears();
}
```

Replace `toSummaryResponse` with this constructor mapping:

```java
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
        age(record.getBirthDate()),
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
```

In `toDetailResponse`, add the new fields immediately after `record.getId()`:

```java
record.getGlobalSequence(),
record.getDisplaySequence(),
record.getCompanyName(),
record.getDepartmentName(),
age(record.getBirthDate()),
```

Then add these editable fields after `record.getAddress()`:

```java
record.getPoliticalStatus(),
record.getFullTimeEducationDegree(),
record.getFullTimeSchool(),
record.getFullTimeMajor(),
record.getPartTimeEducation(),
record.getPartTimeDegree(),
record.getPartTimeSchool(),
record.getPartTimeMajor(),
record.getMaritalStatus(),
record.getRemark(),
```

- [ ] **Step 6: Run the controller test to verify it passes**

Run:

```powershell
$env:JAVA_HOME='D:\AIAPP\.jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -Dtest=AppointmentControllerTest#superadminCanCreateListReadUpdateAndDeleteAppointmentRecord test
```

Expected: PASS.

- [ ] **Step 7: Commit Task 2**

```powershell
git add backend/src/main/java/com/company/admin/appointment backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java
git commit -m "feat: map appointment board fields"
```

---

### Task 3: 后端序号生成、删除重排和部门校验

**Files:**
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentRecordRepository.java`
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`
- Modify: `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`

- [ ] **Step 1: Write failing tests for sequences and department validation**

Add these tests to `AppointmentControllerTest`:

```java
@Test
void createAssignsCompanyGlobalSequenceAndBoardDisplaySequence() throws Exception {
    String token = loginSuperadmin().token();

    Long firstId = createAppointment(token, createRequest("first", "reason", List.of()));
    Long secondId = createAppointment(token, createRequest("second", "reason", List.of()));

    mockMvc.perform(get("/api/party-hr/appointments")
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.id==" + firstId + ")].globalSequence").value(1))
            .andExpect(jsonPath("$.data.items[?(@.id==" + secondId + ")].globalSequence").value(2))
            .andExpect(jsonPath("$.data.items[?(@.id==" + firstId + ")].displaySequence").value(1))
            .andExpect(jsonPath("$.data.items[?(@.id==" + secondId + ")].displaySequence").value(2));
}

@Test
void deleteReordersBoardDisplaySequence() throws Exception {
    String token = loginSuperadmin().token();
    Long firstId = createAppointment(token, createRequest("first", "reason", List.of()));
    Long secondId = createAppointment(token, createRequest("second", "reason", List.of()));
    Long thirdId = createAppointment(token, createRequest("third", "reason", List.of()));

    mockMvc.perform(delete("/api/party-hr/appointments/{id}", secondId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

    mockMvc.perform(get("/api/party-hr/appointments")
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.id==" + firstId + ")].displaySequence").value(1))
            .andExpect(jsonPath("$.data.items[?(@.id==" + thirdId + ")].displaySequence").value(2));
}

@Test
void rejectsAppointmentWithUnknownBoardDepartment() throws Exception {
    String token = loginSuperadmin().token();
    Map<String, Object> request = mutableCreateRequest();
    request.put("departmentName", "未知部门");

    mockMvc.perform(post("/api/party-hr/appointments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("所属部门")));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='D:\AIAPP\.jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -Dtest=AppointmentControllerTest test
```

Expected: FAIL because sequence generation and delete reordering are not implemented.

- [ ] **Step 3: Add repository methods**

Update `AppointmentRecordRepository.java`:

```java
@Query("select coalesce(max(a.globalSequence), 0) from AppointmentRecord a where a.deleted = false and a.companyName = :companyName")
long maxGlobalSequenceByCompanyName(String companyName);

@Query("select coalesce(max(a.displaySequence), 0) from AppointmentRecord a where a.deleted = false")
long maxDisplaySequence();

List<AppointmentRecord> findByDeletedFalseOrderByDisplaySequenceAscIdAsc();
```

- [ ] **Step 4: Implement sequence assignment and delete reordering**

In `AppointmentService.create`, after `mapEditableFields(record, request)` and before save:

```java
assignSequences(record);
```

Add methods:

```java
private void assignSequences(AppointmentRecord record) {
    // 总序号按所属公司单独递增，便于后续新增同层级公司时各自从 1 开始建账。
    record.setGlobalSequence(appointmentRecordRepository.maxGlobalSequenceByCompanyName(record.getCompanyName()) + 1);
    // 序号代表当前党群任免看板整体顺序，新增记录追加到最后。
    record.setDisplaySequence(appointmentRecordRepository.maxDisplaySequence() + 1);
}

private void reorderDisplaySequences() {
    List<AppointmentRecord> activeRecords = appointmentRecordRepository.findByDeletedFalseOrderByDisplaySequenceAscIdAsc();
    for (int index = 0; index < activeRecords.size(); index++) {
        activeRecords.get(index).setDisplaySequence(index + 1);
    }
}
```

In `delete`, after `record.setUpdatedBy(operator.getId())`:

```java
reorderDisplaySequences();
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```powershell
$env:JAVA_HOME='D:\AIAPP\.jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -Dtest=AppointmentControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 3**

```powershell
git add backend/src/main/java/com/company/admin/appointment backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java
git commit -m "feat: manage appointment board sequences"
```

---

### Task 4: 前端类型、默认值和保存清洗

**Files:**
- Modify: `frontend/src/types/appointment.ts`
- Create: `frontend/src/types/appointment.spec.ts`

- [ ] **Step 1: Write failing type behavior tests**

Create `frontend/src/types/appointment.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'

import {
  PARTY_HR_BOARD_DEPARTMENTS,
  createEmptyAppointmentForm,
  normalizeAppointmentForm,
  sanitizeAppointmentPayload
} from './appointment'

describe('appointment form model', () => {
  it('defaults company and department for a new party hr appointment', () => {
    const form = createEmptyAppointmentForm()

    expect(form.companyName).toBe('集团公司')
    expect(form.departmentName).toBe('党群人力部')
  })

  it('exposes the confirmed party hr board department options', () => {
    expect(PARTY_HR_BOARD_DEPARTMENTS).toEqual([
      '领导班子',
      '专家顾问',
      '董事会办公室',
      '财务管理部',
      '纪检监察部',
      '党群人力部',
      '综合管理部',
      '融资管理部',
      '产业发展部',
      '法务风控部',
      '建设管理部'
    ])
  })

  it('normalizes new board fields without dropping user input', () => {
    const normalized = normalizeAppointmentForm({
      companyName: '集团公司',
      departmentName: '领导班子',
      politicalStatus: '中共党员',
      fullTimeEducationDegree: '学士',
      fullTimeSchool: '中国人民大学',
      fullTimeMajor: '行政管理',
      partTimeEducation: '硕士研究生',
      partTimeDegree: '硕士',
      partTimeSchool: '中央党校',
      partTimeMajor: '经济管理',
      maritalStatus: '已婚',
      remark: '备注'
    })

    expect(normalized.departmentName).toBe('领导班子')
    expect(normalized.partTimeMajor).toBe('经济管理')
    expect(normalized.remark).toBe('备注')
  })

  it('sanitizes new board fields in the payload', () => {
    const payload = sanitizeAppointmentPayload({
      name: '张三',
      companyName: '集团公司',
      departmentName: '党群人力部',
      currentPosition: '党群主管',
      phone: '13900001111',
      idCard: '110101199001011234',
      remark: '备注'
    })

    expect(payload.companyName).toBe('集团公司')
    expect(payload.departmentName).toBe('党群人力部')
    expect(payload.currentPosition).toBe('党群主管')
    expect(payload.remark).toBe('备注')
  })
})
```

- [ ] **Step 2: Run the frontend type test to verify it fails**

Run:

```powershell
npm run test -- src/types/appointment.spec.ts
```

Expected: FAIL because the new constants and fields do not exist.

- [ ] **Step 3: Expand appointment types and defaults**

Update `frontend/src/types/appointment.ts`:

```ts
export const DEFAULT_COMPANY_NAME = '集团公司'
export const DEFAULT_BOARD_DEPARTMENT = '党群人力部'

export const PARTY_HR_BOARD_DEPARTMENTS = [
  '领导班子',
  '专家顾问',
  '董事会办公室',
  '财务管理部',
  '纪检监察部',
  '党群人力部',
  '综合管理部',
  '融资管理部',
  '产业发展部',
  '法务风控部',
  '建设管理部'
] as const
```

Update `AppointmentSummary`:

```ts
export interface AppointmentSummary {
  id: number
  globalSequence: number
  displaySequence: number
  companyName: string
  departmentName: string
  name: string
  currentPosition: string
  gender: string
  ethnicity: string
  idCard: string
  age: number | null
  politicalStatus: string
  fullTimeEducation: string
  fullTimeEducationDegree: string
  fullTimeSchool: string
  fullTimeMajor: string
  partTimeEducation: string
  partTimeDegree: string
  partTimeSchool: string
  partTimeMajor: string
  technicalPosition: string
  phone: string
  maritalStatus: string
  remark: string
}
```

Add these fields to `AppointmentFormPayload`:

```ts
companyName: string
departmentName: string
politicalStatus: string
fullTimeEducationDegree: string
fullTimeSchool: string
fullTimeMajor: string
partTimeEducation: string
partTimeDegree: string
partTimeSchool: string
partTimeMajor: string
maritalStatus: string
remark: string
```

Add default values in `createEmptyAppointmentForm`:

```ts
companyName: DEFAULT_COMPANY_NAME,
departmentName: DEFAULT_BOARD_DEPARTMENT,
politicalStatus: '',
fullTimeEducationDegree: '',
fullTimeSchool: '',
fullTimeMajor: '',
partTimeEducation: '',
partTimeDegree: '',
partTimeSchool: '',
partTimeMajor: '',
maritalStatus: '',
remark: '',
```

- [ ] **Step 4: Run the type test to verify it passes**

Run:

```powershell
npm run test -- src/types/appointment.spec.ts
```

Expected: PASS.

- [ ] **Step 5: Commit Task 4**

```powershell
git add frontend/src/types/appointment.ts frontend/src/types/appointment.spec.ts
git commit -m "feat: add appointment board frontend types"
```

---

### Task 5: 任免审批表弹窗字段升级

**Files:**
- Modify: `frontend/src/components/appointment/AppointmentFormGrid.vue`
- Modify: `frontend/src/components/appointment/AppointmentFormGrid.spec.ts`
- Modify: `frontend/src/components/appointment/AppointmentFormDialog.vue`

- [ ] **Step 1: Write failing form grid tests**

Update `AppointmentFormGrid.spec.ts` first test with:

```ts
expect(text).toContain('所属公司')
expect(text).toContain('所属部门')
expect(text).toContain('政治面貌')
expect(text).toContain('婚姻状况')
expect(text).toContain('备注')
expect(text).toContain('学位（全日制）')
expect(text).toContain('毕业院校（全日制）')
expect(text).toContain('专业（全日制）')
expect(text).toContain('学历（非全日制）')
expect(text).toContain('学位（非全日制）')
expect(text).toContain('毕业院校（非全日制）')
expect(text).toContain('专业（非全日制）')
expect(text).toContain('领导班子')
expect(text).toContain('建设管理部')
expect(text).not.toContain('职位')
expect(text).not.toContain('地址')
```

- [ ] **Step 2: Run the form grid test to verify it fails**

Run:

```powershell
npm run test -- src/components/appointment/AppointmentFormGrid.spec.ts
```

Expected: FAIL because the new fields and department radio options are not rendered.

- [ ] **Step 3: Update `AppointmentFormGrid.vue` imports**

Add:

```ts
import { ElRadio, ElRadioGroup } from 'element-plus'
```

Import department options:

```ts
import {
  PARTY_HR_BOARD_DEPARTMENTS,
  createEmptyFamilyMember,
  normalizeAppointmentForm,
  type AppointmentFormMode,
  type AppointmentFormModel,
  type AppointmentFormPayload
} from '@/types/appointment'
```

- [ ] **Step 4: Replace the old top board field grid**

Replace the current `.board-field-grid` content with:

```vue
<div class="board-field-grid">
  <label>
    <span>所属公司</span>
    <el-input v-model="form.companyName" :disabled="isReadonly" />
  </label>
  <label class="department-field">
    <span>所属部门</span>
    <el-radio-group v-model="form.departmentName" :disabled="isReadonly">
      <el-radio v-for="department in PARTY_HR_BOARD_DEPARTMENTS" :key="department" :label="department">
        {{ department }}
      </el-radio>
    </el-radio-group>
  </label>
  <label>
    <span>联系方式（手机长号）</span>
    <el-input v-model="form.phone" :disabled="isReadonly" />
  </label>
  <label>
    <span>身份证号</span>
    <el-input v-model="form.idCard" :disabled="isReadonly" />
  </label>
  <label>
    <span>政治面貌</span>
    <el-input v-model="form.politicalStatus" :disabled="isReadonly" />
  </label>
  <label>
    <span>婚姻状况</span>
    <el-input v-model="form.maritalStatus" :disabled="isReadonly" />
  </label>
  <label class="wide">
    <span>备注</span>
    <el-input v-model="form.remark" :disabled="isReadonly" :rows="2" type="textarea" />
  </label>
</div>
```

- [ ] **Step 5: Replace the education rows**

Replace the two “学历学位” rows with:

```vue
<tr>
  <th rowspan="2">学历学位</th>
  <th>学历（全日制）</th>
  <td><el-input v-model="form.fullTimeEducation" :disabled="isReadonly" /></td>
  <th>学位（全日制）</th>
  <td><el-input v-model="form.fullTimeEducationDegree" :disabled="isReadonly" /></td>
  <th>毕业院校（全日制）</th>
  <td colspan="2"><el-input v-model="form.fullTimeSchool" :disabled="isReadonly" /></td>
</tr>
<tr>
  <th>专业（全日制）</th>
  <td><el-input v-model="form.fullTimeMajor" :disabled="isReadonly" /></td>
  <th>学历（非全日制）</th>
  <td><el-input v-model="form.partTimeEducation" :disabled="isReadonly" /></td>
  <th>学位（非全日制）</th>
  <td colspan="2"><el-input v-model="form.partTimeDegree" :disabled="isReadonly" /></td>
</tr>
<tr>
  <th>毕业院校（非全日制）</th>
  <td colspan="3"><el-input v-model="form.partTimeSchool" :disabled="isReadonly" /></td>
  <th>专业（非全日制）</th>
  <td colspan="3"><el-input v-model="form.partTimeMajor" :disabled="isReadonly" /></td>
</tr>
```

- [ ] **Step 6: Update `AppointmentFormDialog.vue` required validation**

Replace `requiredValues` with:

```ts
const requiredValues = [
  payload.name,
  payload.phone,
  payload.idCard,
  payload.currentPosition,
  payload.companyName,
  payload.departmentName
]
```

Replace the warning message with:

```ts
ElMessage.warning('请填写姓名、联系方式（手机长号）、身份证号、现任职务、所属公司和所属部门')
```

- [ ] **Step 7: Run the form tests to verify they pass**

Run:

```powershell
npm run test -- src/components/appointment/AppointmentFormGrid.spec.ts
```

Expected: PASS.

- [ ] **Step 8: Commit Task 5**

```powershell
git add frontend/src/components/appointment/AppointmentFormGrid.vue frontend/src/components/appointment/AppointmentFormGrid.spec.ts frontend/src/components/appointment/AppointmentFormDialog.vue
git commit -m "feat: update appointment form board fields"
```

---

### Task 6: 任免看板表格表头升级

**Files:**
- Modify: `frontend/src/components/appointment/AppointmentTable.vue`
- Create: `frontend/src/components/appointment/AppointmentTable.spec.ts`
- Modify: `frontend/e2e/group-admin.spec.ts`

- [ ] **Step 1: Write failing table component test**

Create `frontend/src/components/appointment/AppointmentTable.spec.ts`:

```ts
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'

import AppointmentTable from './AppointmentTable.vue'

describe('AppointmentTable', () => {
  it('renders confirmed party hr board headers', () => {
    const wrapper = mount(AppointmentTable, {
      props: {
        appointments: []
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const text = wrapper.text()

    for (const header of [
      '总序号',
      '序号',
      '所属公司',
      '所属部门',
      '姓名',
      '现任职务',
      '性别',
      '民族',
      '身份证号',
      '年龄',
      '政治面貌',
      '全日制教育',
      '学历',
      '学位',
      '毕业院校',
      '专业',
      '非全日制教育',
      '专业技术职称',
      '联系方式（手机长号）',
      '婚姻状况',
      '备注',
      '操作'
    ]) {
      expect(text).toContain(header)
    }

    expect(text).not.toContain('电话')
    expect(text).not.toContain('职位')
    expect(text).not.toContain('地址')
  })
})
```

- [ ] **Step 2: Run the table test to verify it fails**

Run:

```powershell
npm run test -- src/components/appointment/AppointmentTable.spec.ts
```

Expected: FAIL because table headers still use old columns.

- [ ] **Step 3: Update `AppointmentTable.vue` columns**

Replace current columns with:

```vue
<el-table-column fixed label="总序号" min-width="86" prop="globalSequence" />
<el-table-column label="序号" min-width="72" prop="displaySequence" />
<el-table-column label="所属公司" min-width="120" prop="companyName" show-overflow-tooltip />
<el-table-column label="所属部门" min-width="130" prop="departmentName" show-overflow-tooltip />
<el-table-column fixed label="姓名" min-width="110">
  <template #default="{ row }">
    <el-button link type="primary" @click="openViewFromRow(row)">{{ row.name }}</el-button>
  </template>
</el-table-column>
<el-table-column label="现任职务" min-width="160" prop="currentPosition" show-overflow-tooltip />
<el-table-column label="性别" min-width="70" prop="gender" />
<el-table-column label="民族" min-width="90" prop="ethnicity" />
<el-table-column label="身份证号" min-width="190" prop="idCard" show-overflow-tooltip />
<el-table-column label="年龄" min-width="70" prop="age" />
<el-table-column label="政治面貌" min-width="120" prop="politicalStatus" show-overflow-tooltip />
<el-table-column label="全日制教育">
  <el-table-column label="学历" min-width="100" prop="fullTimeEducation" show-overflow-tooltip />
  <el-table-column label="学位" min-width="100" prop="fullTimeEducationDegree" show-overflow-tooltip />
  <el-table-column label="毕业院校" min-width="160" prop="fullTimeSchool" show-overflow-tooltip />
  <el-table-column label="专业" min-width="140" prop="fullTimeMajor" show-overflow-tooltip />
</el-table-column>
<el-table-column label="非全日制教育">
  <el-table-column label="学历" min-width="120" prop="partTimeEducation" show-overflow-tooltip />
  <el-table-column label="学位" min-width="100" prop="partTimeDegree" show-overflow-tooltip />
  <el-table-column label="毕业院校" min-width="160" prop="partTimeSchool" show-overflow-tooltip />
  <el-table-column label="专业" min-width="140" prop="partTimeMajor" show-overflow-tooltip />
</el-table-column>
<el-table-column label="专业技术职称" min-width="140" prop="technicalPosition" show-overflow-tooltip />
<el-table-column label="联系方式（手机长号）" min-width="170" prop="phone" show-overflow-tooltip />
<el-table-column label="婚姻状况" min-width="100" prop="maritalStatus" />
<el-table-column label="备注" min-width="160" prop="remark" show-overflow-tooltip />
```

Keep the existing fixed right operation column unchanged.

- [ ] **Step 4: Update E2E header assertions**

Replace the old header list in `frontend/e2e/group-admin.spec.ts` with:

```ts
for (const header of [
  '总序号',
  '序号',
  '所属公司',
  '所属部门',
  '姓名',
  '现任职务',
  '性别',
  '民族',
  '身份证号',
  '年龄',
  '政治面貌',
  '全日制教育',
  '非全日制教育',
  '专业技术职称',
  '联系方式（手机长号）',
  '婚姻状况',
  '备注',
  '操作'
]) {
  await expect(page.locator('.el-table__header').getByText(header, { exact: true })).toBeVisible()
}

for (const removedHeader of ['电话', '职位', '地址']) {
  await expect(page.locator('.el-table__header').getByText(removedHeader, { exact: true })).toHaveCount(0)
}
```

- [ ] **Step 5: Run table test to verify it passes**

Run:

```powershell
npm run test -- src/components/appointment/AppointmentTable.spec.ts
```

Expected: PASS.

- [ ] **Step 6: Commit Task 6**

```powershell
git add frontend/src/components/appointment/AppointmentTable.vue frontend/src/components/appointment/AppointmentTable.spec.ts frontend/e2e/group-admin.spec.ts
git commit -m "feat: update appointment board table headers"
```

---

### Task 7: Full verification and Docker deployment refresh

**Files:**
- No source changes expected unless verification finds a bug.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
$env:JAVA_HOME='D:\AIAPP\.jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd test
```

Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 2: Run frontend unit tests**

Run:

```powershell
npm run test
```

Expected: all Vitest tests pass.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm run build
```

Expected: production build completes.

- [ ] **Step 4: Rebuild local Docker deployment**

Run from repository root:

```powershell
$env:Path = 'C:\Program Files\Docker\Docker\resources\bin;' + $env:Path; docker compose up -d --build
```

Expected: `mysql`, `backend`, and `frontend` containers are running; frontend exposes `0.0.0.0:8080->80/tcp`.

- [ ] **Step 5: Run Playwright E2E against Docker deployment**

Run:

```powershell
npm run e2e
```

Expected: Playwright passes with the updated header assertions.

- [ ] **Step 6: Confirm local server URL**

Run:

```powershell
Invoke-WebRequest -Uri 'http://127.0.0.1:8080/login' -UseBasicParsing
Invoke-WebRequest -Uri 'http://192.168.0.102:8080/login' -UseBasicParsing
```

Expected: both return HTTP 200 if the machine is still on IP `192.168.0.102`; if the LAN IP changed, report the new IP from `Get-NetIPAddress -AddressFamily IPv4`.

- [ ] **Step 7: Confirm no unexpected working tree changes**

Run:

```powershell
git status --short
```

Expected: no output. If verification finds a bug, stop and create a focused follow-up fix task using TDD instead of committing an unspecified patch.
