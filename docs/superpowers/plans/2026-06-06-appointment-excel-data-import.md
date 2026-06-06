# 任免数据 Excel 一次性导入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Excel 第一张工作表中的 75 条任免数据安全导入当前 MySQL，并让任免看板和任免审批表按原始字段正确展示。

**Architecture:** 仓库内通过 Flyway 增加可空持久年龄字段，后端响应优先使用持久年龄、缺失时沿用出生日期动态计算。业务数据通过本机生成的一次性事务 SQL 导入当前 Docker MySQL，SQL、回滚文件和备份均保留在 `D:\AIAPP\素材`，不提交包含身份证信息的文件。

**Tech Stack:** Java 17、Spring Boot 4、Spring Data JPA、Flyway、MySQL 8.4、JUnit 5、MockMvc、Docker Compose、`@oai/artifact-tool`

---

## 文件边界

- Create: `backend/src/main/resources/db/migration/V6__add_appointment_age.sql`
  - 仅增加 `appointment_record.age` 可空列。
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentRecord.java`
  - 映射持久年龄字段并提供访问器。
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`
  - 列表和详情优先返回持久年龄，空值时继续动态计算。
- Modify: `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`
  - 覆盖持久年龄优先、普通新增动态年龄和编辑不覆盖年龄。
- Create locally, do not commit: `D:\AIAPP\素材\appointment-import-work\generate-appointment-import.mjs`
  - 读取 Excel、校验 75 条数据并生成导入和回滚 SQL。
- Create locally, do not commit: `D:\AIAPP\素材\任免数据导入-20260606.sql`
  - 当前 MySQL 的一次性事务导入文件。
- Create locally, do not commit: `D:\AIAPP\素材\任免数据回滚-20260606.sql`
  - 仅删除本批次数据的回滚文件。
- Create locally, do not commit: `D:\AIAPP\素材\group-admin-before-appointment-import-20260606.sql`
  - 导入前数据库备份。
- Preserve without modification or commit: `frontend/src/views/settings/SystemLogsView.vue`
  - 当前工作区已有其他任务的未提交修改。

### Task 1: 以失败测试锁定持久年龄行为

**Files:**
- Modify: `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`

- [ ] **Step 1: 添加持久年龄优先且编辑后保留的接口测试**

在现有年龄测试附近新增：

```java
@Test
void persistedAgeTakesPrecedenceAndSurvivesEditableUpdate() throws Exception {
    String token = loginSuperadmin().token();
    Long appointmentId = createAppointment(
            token,
            createRequest("导入年龄测试", "测试持久年龄", List.of()));

    jdbcTemplate.update(
            "UPDATE appointment_record SET age = ? WHERE id = ?",
            52,
            appointmentId);

    mockMvc.perform(get("/api/party-hr/appointments")
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].age").value(52));

    mockMvc.perform(get("/api/party-hr/appointments/{id}", appointmentId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.age").value(52));

    Map<String, Object> updatedRequest = mutableCreateRequest();
    updatedRequest.put("name", "导入年龄测试-已编辑");

    mockMvc.perform(put("/api/party-hr/appointments/{id}", appointmentId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.age").value(52));
}
```

- [ ] **Step 2: 运行测试并确认按预期失败**

Run:

```powershell
.\mvnw.cmd -Dtest=AppointmentControllerTest#persistedAgeTakesPrecedenceAndSurvivesEditableUpdate test
```

Working directory: `backend`

Expected: FAIL，错误明确指向 `appointment_record.age` 列不存在，而不是登录、测试数据或 JSON 断言错误。

### Task 2: 增加年龄列并保持现有动态年龄兼容

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__add_appointment_age.sql`
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentRecord.java`
- Modify: `backend/src/main/java/com/company/admin/appointment/AppointmentService.java`
- Test: `backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java`

- [ ] **Step 1: 添加 Flyway 结构迁移**

```sql
ALTER TABLE appointment_record
  ADD COLUMN age INT NULL;
```

- [ ] **Step 2: 在实体中映射持久年龄**

在 `birthDate` 后增加字段：

```java
@Column
private Integer age;
```

在 `getBirthDate`/`setBirthDate` 附近增加：

```java
public Integer getAge() {
    return age;
}

public void setAge(Integer age) {
    this.age = age;
}
```

- [ ] **Step 3: 增加统一年龄解析方法**

在 `AppointmentService` 中新增：

```java
private Integer resolveAge(AppointmentRecord record) {
    // Excel 历史台账年龄需保持原值；普通新增记录未保存年龄时仍按出生日期计算周岁。
    if (record.getAge() != null) {
        return record.getAge();
    }
    return calculateAge(record.getBirthDate());
}
```

将 `toSummaryResponse` 和 `toDetailResponse` 中的：

```java
calculateAge(record.getBirthDate())
```

替换为：

```java
resolveAge(record)
```

不要在 `mapEditableFields` 中设置 `age`，确保普通新增时为空、编辑导入记录时保留原始年龄。

- [ ] **Step 4: 运行目标测试确认转绿**

Run:

```powershell
.\mvnw.cmd -Dtest=AppointmentControllerTest#persistedAgeTakesPrecedenceAndSurvivesEditableUpdate test
```

Expected: PASS。

- [ ] **Step 5: 运行完整任免接口测试**

Run:

```powershell
.\mvnw.cmd -Dtest=AppointmentControllerTest test
```

Expected: 全部 PASS；既有 `EXPECTED_AGE_ON_FIXED_CLOCK`、出生日期为空和未来日期测试继续通过。

- [ ] **Step 6: 提交年龄兼容代码**

```powershell
git add backend/src/main/resources/db/migration/V6__add_appointment_age.sql `
  backend/src/main/java/com/company/admin/appointment/AppointmentRecord.java `
  backend/src/main/java/com/company/admin/appointment/AppointmentService.java `
  backend/src/test/java/com/company/admin/appointment/AppointmentControllerTest.java
git commit -m "feat: preserve imported appointment ages"
```

### Task 3: 生成本机一次性导入 SQL 和回滚 SQL

**Files:**
- Create locally: `D:\AIAPP\素材\appointment-import-work\generate-appointment-import.mjs`
- Create locally: `D:\AIAPP\素材\任免数据导入-20260606.sql`
- Create locally: `D:\AIAPP\素材\任免数据回滚-20260606.sql`

- [ ] **Step 1: 创建隔离的本机生成目录**

```powershell
New-Item -ItemType Directory -Force 'D:\AIAPP\素材\appointment-import-work'
New-Item -ItemType Junction `
  -Path 'D:\AIAPP\素材\appointment-import-work\node_modules' `
  -Target 'C:\Users\HUAWEI\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules'
```

该目录位于 Git 仓库外，禁止将身份证号和导入 SQL 加入版本控制。

- [ ] **Step 2: 编写工作簿转换脚本**

脚本核心必须包含以下规则：

```js
import fs from 'node:fs/promises'
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool'

const SOURCE = 'D:/AIAPP/素材/导入数据.xlsx'
const IMPORT_SQL = 'D:/AIAPP/素材/任免数据导入-20260606.sql'
const ROLLBACK_SQL = 'D:/AIAPP/素材/任免数据回滚-20260606.sql'
const BATCH_TIME = '2026-06-06 12:00:00'
const ALLOWED_DEPARTMENTS = new Set([
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

const normalizeText = (value) => {
  if (value === null || value === undefined) return null
  return String(value).replace(/\r\n/g, '\n').trim()
}

const sqlText = (value) => {
  const normalized = normalizeText(value)
  if (normalized === null || normalized === '') return 'NULL'
  return `'${normalized.replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

const sqlRequiredText = (value) => {
  const normalized = normalizeText(value)
  return `'${(normalized ?? '').replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

const normalizeAge = (value) => {
  if (typeof value === 'number') return value
  const dateValue = new Date(value)
  if (Number.isNaN(dateValue.getTime())) {
    throw new Error(`无法识别年龄值: ${value}`)
  }
  return Math.round((dateValue.getTime() - Date.UTC(1899, 11, 30)) / 86_400_000)
}

const birthDateFromIdCard = (idCard) => {
  const value = String(idCard)
  if (!/^\d{17}[\dXx]$/.test(value)) {
    throw new Error(`身份证号格式错误: ${value}`)
  }
  return `${value.slice(6, 10)}-${value.slice(10, 12)}-${value.slice(12, 14)}`
}

const input = await FileBlob.load(SOURCE)
const workbook = await SpreadsheetFile.importXlsx(input)
const inspected = await workbook.inspect({
  kind: 'table',
  range: '导入数据!A1:W77',
  include: 'values,formulas',
  tableMaxRows: 80,
  tableMaxCols: 23,
  tableMaxCellChars: 500,
  maxChars: 200000
})
const values = JSON.parse(inspected.ndjson).values

let companyName = ''
let departmentName = ''
const rows = values.slice(2)
  .filter((row) => row.some((value) => value !== null && value !== ''))
  .map((row, index) => {
    companyName = normalizeText(row[2]) || companyName
    departmentName = normalizeText(row[3]) || departmentName
    return {
      excelRow: index + 3,
      globalSequence: Number(row[0]),
      displaySequence: Number(row[1]),
      companyName,
      departmentName,
      name: normalizeText(row[4]),
      currentPosition: normalizeText(row[5]),
      gender: normalizeText(row[6]),
      ethnicity: normalizeText(row[7]),
      idCard: normalizeText(row[8]),
      age: normalizeAge(row[9]),
      politicalStatus: normalizeText(row[10]),
      fullTimeEducation: normalizeText(row[11]),
      fullTimeEducationDegree: normalizeText(row[12]),
      fullTimeSchool: normalizeText(row[13]),
      fullTimeMajor: normalizeText(row[14]),
      partTimeEducation: normalizeText(row[15]),
      partTimeDegree: normalizeText(row[16]),
      partTimeSchool: normalizeText(row[17]),
      partTimeMajor: normalizeText(row[18]),
      technicalPosition: normalizeText(row[19]),
      phone: normalizeText(row[20]) ?? '',
      maritalStatus: normalizeText(row[21]),
      remark: normalizeText(row[22])
    }
  })

if (rows.length !== 75) throw new Error(`期望 75 条数据，实际 ${rows.length} 条`)
rows.forEach((row, index) => {
  if (row.globalSequence !== 1 || row.displaySequence !== index + 1) {
    throw new Error(`第 ${row.excelRow} 行序号不符合预期`)
  }
  if (!row.companyName || !row.departmentName || !row.name || !row.currentPosition) {
    throw new Error(`第 ${row.excelRow} 行缺少必需映射字段`)
  }
  if (!ALLOWED_DEPARTMENTS.has(row.departmentName)) {
    throw new Error(`第 ${row.excelRow} 行部门不在系统选项中`)
  }
  if (!Number.isInteger(row.age) || row.age < 0 || row.age > 150) {
    throw new Error(`第 ${row.excelRow} 行年龄不合法`)
  }
  const birthDate = birthDateFromIdCard(row.idCard)
  const parsedBirthDate = new Date(`${birthDate}T00:00:00Z`)
  if (Number.isNaN(parsedBirthDate.getTime()) || parsedBirthDate.toISOString().slice(0, 10) !== birthDate) {
    throw new Error(`第 ${row.excelRow} 行身份证出生日期不合法`)
  }
})

const limits = {
  companyName: 120,
  departmentName: 120,
  name: 64,
  currentPosition: 255,
  gender: 20,
  ethnicity: 60,
  idCard: 32,
  politicalStatus: 80,
  fullTimeEducation: 160,
  fullTimeEducationDegree: 120,
  fullTimeSchool: 160,
  fullTimeMajor: 160,
  partTimeEducation: 160,
  partTimeDegree: 120,
  partTimeSchool: 160,
  partTimeMajor: 160,
  technicalPosition: 120,
  phone: 32,
  maritalStatus: 40
}

rows.forEach((row) => {
  Object.entries(limits).forEach(([field, limit]) => {
    const length = String(row[field] ?? '').length
    if (length > limit) {
      throw new Error(`第 ${row.excelRow} 行 ${field} 长度 ${length} 超过限制 ${limit}`)
    }
  })
})

const columns = [
  'global_sequence',
  'display_sequence',
  'company_name',
  'department_name',
  'name',
  'phone',
  'id_card',
  'position_name',
  'graduation_school',
  'address',
  'gender',
  'birth_date',
  'age',
  'ethnicity',
  'political_status',
  'technical_position',
  'full_time_education',
  'full_time_education_degree',
  'full_time_school',
  'full_time_major',
  'part_time_education',
  'part_time_degree',
  'part_time_school',
  'part_time_major',
  'current_position',
  'marital_status',
  'remark',
  'approval_authority_opinion',
  'created_by',
  'updated_by',
  'created_at',
  'updated_at',
  'deleted'
]

const operatorIdSql =
  "(SELECT id FROM sys_user WHERE username = 'superadmin' AND deleted = FALSE LIMIT 1)"

const insertStatements = rows.map((row) => {
  const values = [
    row.globalSequence,
    row.displaySequence,
    sqlRequiredText(row.companyName),
    sqlRequiredText(row.departmentName),
    sqlRequiredText(row.name),
    "''",
    sqlRequiredText(row.idCard),
    "''",
    "''",
    "''",
    sqlText(row.gender),
    sqlText(birthDateFromIdCard(row.idCard)),
    row.age,
    sqlText(row.ethnicity),
    sqlText(row.politicalStatus),
    sqlText(row.technicalPosition),
    sqlText(row.fullTimeEducation),
    sqlText(row.fullTimeEducationDegree),
    sqlText(row.fullTimeSchool),
    sqlText(row.fullTimeMajor),
    sqlText(row.partTimeEducation),
    sqlText(row.partTimeDegree),
    sqlText(row.partTimeSchool),
    sqlText(row.partTimeMajor),
    sqlRequiredText(row.currentPosition),
    sqlText(row.maritalStatus),
    sqlText(row.remark),
    "'此表信息已认定'",
    operatorIdSql,
    operatorIdSql,
    `'${BATCH_TIME}'`,
    `'${BATCH_TIME}'`,
    'FALSE'
  ]

  return `INSERT INTO appointment_record (${columns.join(', ')})\nVALUES (${values.join(', ')});`
}).join('\n\n')

const importSql = `SET NAMES utf8mb4;
START TRANSACTION;

CREATE TEMPORARY TABLE appointment_import_guard (
  valid_flag TINYINT NOT NULL CHECK (valid_flag = 1)
);

INSERT INTO appointment_import_guard(valid_flag)
SELECT IF(
  (SELECT COUNT(*) FROM appointment_record WHERE deleted = FALSE) = 0
  AND (SELECT COUNT(*) FROM sys_user WHERE username = 'superadmin' AND deleted = FALSE) = 1,
  1,
  0
);

${insertStatements}

INSERT INTO appointment_import_guard(valid_flag)
SELECT IF(
  (SELECT COUNT(*)
     FROM appointment_record
    WHERE created_at = '${BATCH_TIME}'
      AND created_by = ${operatorIdSql}
      AND deleted = FALSE) = 75,
  1,
  0
);

COMMIT;
`

const rollbackSql = `SET NAMES utf8mb4;
START TRANSACTION;

CREATE TEMPORARY TABLE appointment_rollback_guard (
  valid_flag TINYINT NOT NULL CHECK (valid_flag = 1)
);

INSERT INTO appointment_rollback_guard(valid_flag)
SELECT IF(
  (SELECT COUNT(*)
     FROM appointment_record
    WHERE created_at = '${BATCH_TIME}'
      AND created_by = ${operatorIdSql}
      AND deleted = FALSE) = 75,
  1,
  0
);

DELETE FROM appointment_record
WHERE created_at = '${BATCH_TIME}'
  AND created_by = ${operatorIdSql}
  AND display_sequence BETWEEN 1 AND 75;

INSERT INTO appointment_rollback_guard(valid_flag)
SELECT IF(ROW_COUNT() = 75, 1, 0);

COMMIT;
`

await fs.writeFile(IMPORT_SQL, importSql, 'utf8')
await fs.writeFile(ROLLBACK_SQL, rollbackSql, 'utf8')
console.log(`已生成 ${rows.length} 条任免数据导入 SQL`)
```

- [ ] **Step 3: 运行生成脚本**

Run:

```powershell
& 'C:\Users\HUAWEI\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' `
  'D:\AIAPP\素材\appointment-import-work\generate-appointment-import.mjs'
```

Expected:

```text
已生成 75 条任免数据导入 SQL
```

- [ ] **Step 4: 检查生成结果但不输出身份证明文**

检查文件存在、`INSERT INTO appointment_record` 数量为 75、部门分布和序号校验通过。终端输出只能展示统计信息，不打印完整 SQL 或身份证号。

### Task 4: 完整后端验证并更新 Docker 数据库结构

**Files:**
- Verify: `backend`
- Runtime: Docker Compose

- [ ] **Step 1: 运行完整后端测试**

```powershell
.\mvnw.cmd test
```

Working directory: `backend`

Expected: BUILD SUCCESS，0 failures，0 errors。

- [ ] **Step 2: 构建后端生产包**

```powershell
.\mvnw.cmd -DskipTests package
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 重建并启动后端**

```powershell
docker compose up -d --build backend
docker compose ps
```

Expected: `mysql` healthy，`backend` Up，`frontend` 保持 Up。

- [ ] **Step 4: 确认 Flyway 已增加年龄列**

从 `.env` 读取数据库账号后执行：

```sql
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'appointment_record'
  AND COLUMN_NAME = 'age';
```

Expected: 一行，`age | int | YES`。

### Task 5: 备份并导入当前 MySQL

**Files:**
- Create locally: `D:\AIAPP\素材\group-admin-before-appointment-import-20260606.sql`
- Execute locally: `D:\AIAPP\素材\任免数据导入-20260606.sql`

- [ ] **Step 1: 再次确认导入前有效记录为 0**

```sql
SELECT COUNT(*) FROM appointment_record WHERE deleted = FALSE;
```

Expected: `0`。若不是 0，立即停止，不执行后续导入。

- [ ] **Step 2: 创建导入前备份**

使用 `.env` 中数据库名和账号，通过容器内 `mysqldump` 输出到本机：

```powershell
$db = @{}
Get-Content .env | ForEach-Object {
  if ($_ -match '^([^#=]+)=(.*)$') {
    $db[$matches[1]] = $matches[2]
  }
}
$mysqlContainer = docker compose ps -q mysql
docker compose exec -T mysql sh -c "mysqldump --default-character-set=utf8mb4 -u'$($db.MYSQL_USER)' -p'$($db.MYSQL_PASSWORD)' '$($db.MYSQL_DATABASE)' > /tmp/group-admin-before-appointment-import-20260606.sql"
docker cp "${mysqlContainer}:/tmp/group-admin-before-appointment-import-20260606.sql" 'D:\AIAPP\素材\group-admin-before-appointment-import-20260606.sql'
docker compose exec -T mysql rm -f /tmp/group-admin-before-appointment-import-20260606.sql
if ((Get-Item 'D:\AIAPP\素材\group-admin-before-appointment-import-20260606.sql').Length -le 0) {
  throw '数据库备份文件为空'
}
```

确认文件大小大于 0，且文件保留在 Git 仓库外。

- [ ] **Step 3: 执行一次性事务导入**

```powershell
docker cp 'D:\AIAPP\素材\任免数据导入-20260606.sql' "${mysqlContainer}:/tmp/appointment-import-20260606.sql"
docker compose exec -T mysql sh -c "mysql --default-character-set=utf8mb4 -u'$($db.MYSQL_USER)' -p'$($db.MYSQL_PASSWORD)' '$($db.MYSQL_DATABASE)' < /tmp/appointment-import-20260606.sql"
docker compose exec -T mysql rm -f /tmp/appointment-import-20260606.sql
```

Expected: 命令退出码 0，没有约束错误或事务回滚。

- [ ] **Step 4: 核对数据库汇总**

执行：

```sql
SELECT
  COUNT(*) AS total,
  MIN(display_sequence) AS min_sequence,
  MAX(display_sequence) AS max_sequence,
  COUNT(DISTINCT display_sequence) AS unique_sequences,
  COUNT(DISTINCT company_name) AS company_count,
  COUNT(DISTINCT department_name) AS department_count,
  SUM(phone = '') AS empty_phone_count,
  SUM(age IS NULL) AS null_age_count
FROM appointment_record
WHERE deleted = FALSE;
```

Expected:

```text
total=75
min_sequence=1
max_sequence=75
unique_sequences=75
company_count=1
department_count=11
empty_phone_count=75
null_age_count=0
```

- [ ] **Step 5: 核对部门分布和三条抽样**

对比 Excel 与数据库的部门计数；抽查 `display_sequence` 为 1、38、75 的记录，核对姓名、身份证号、出生年月、年龄、教育字段、职称、婚姻状态。验证输出中身份证号只显示后四位。

### Task 6: 验证列表接口、详情接口和浏览器页面

**Files:**
- Runtime verification only

- [ ] **Step 1: 登录并获取超级管理员令牌**

调用：

```http
POST http://127.0.0.1:8080/api/auth/login
Content-Type: application/json

{"username":"superadmin","password":"xjyadmin"}
```

Expected: HTTP 200，响应包含令牌。

- [ ] **Step 2: 验证任免看板列表接口**

调用：

```http
GET http://127.0.0.1:8080/api/party-hr/appointments?page=0&size=100
Authorization: Bearer <token>
```

Expected:

- `total` 为 75。
- 第一条 `globalSequence=1`、`displaySequence=1`。
- 看板 23 个 Excel 数据字段均能通过现有摘要响应返回。
- 第一条年龄使用 Excel 原值 52，而不是按 2026 年 6 月计算的周岁 51。

- [ ] **Step 3: 验证任免审批表详情接口**

使用第一条记录 ID 调用详情接口，确认：

```text
companyName=集团公司
departmentName=领导班子
name=何劲松
birthDate=1974-11-30
age=52
phone=""
approvalAuthorityOpinion=此表信息已认定
```

同时核对 Excel 提供的现任职务、政治面貌、教育、职称和婚姻状态；Excel 未提供字段为空。

- [ ] **Step 4: 在浏览器完成可视化验证**

打开 `http://127.0.0.1:8080/`，使用 `superadmin` 登录：

1. 进入“党群人力部 > 任免看板”。
2. 确认总计 75 条，首条表格字段与 Excel 一致。
3. 点击首条姓名，确认任免审批表的所属公司、所属部门、姓名、身份证号、出生年月、政治面貌、教育信息、现任职务、专业技术职务和婚姻状况。
4. 截图保存到 `D:\AIAPP\appointment-import-verification.png`。

### Task 7: 最终检查和提交

**Files:**
- Verify all tracked changes

- [ ] **Step 1: 运行最终验证**

```powershell
.\mvnw.cmd test
git diff --check
docker compose ps
```

Expected: 后端测试全部通过；`git diff --check` 无错误；三个 Docker 服务正常运行。

- [ ] **Step 2: 确认敏感数据未进入 Git**

```powershell
git status --short
git diff --name-only HEAD
git ls-files | Select-String '任免数据导入|导入数据.xlsx|appointment-import-work'
```

Expected:

- Git 中没有 Excel、生成 SQL、数据库备份或身份证数据文件。
- `frontend/src/views/settings/SystemLogsView.vue` 仍保持用户已有未提交状态，不包含在本任务提交中。

- [ ] **Step 3: 检查提交边界**

确认 Task 2 的代码提交已包含迁移、实体、服务和测试。若验证阶段产生必要修正，只能显式添加上述四个后端文件并单独提交；禁止执行 `git add .`，避免纳入系统日志页面和本机敏感文件。
