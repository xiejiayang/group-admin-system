<script setup lang="ts">
import { ElButton, ElEmpty, ElTable, ElTableColumn } from 'element-plus'

import type { AppointmentSummary } from '@/types/appointment'

defineProps<{
  appointments: AppointmentSummary[]
  loading?: boolean
}>()

const emit = defineEmits<{
  view: [appointment: AppointmentSummary]
  edit: [appointment: AppointmentSummary]
  delete: [appointment: AppointmentSummary]
}>()

const openView = (appointment: AppointmentSummary) => {
  emit('view', appointment)
}

const openViewFromRow = (row: unknown) => {
  openView(row as AppointmentSummary)
}

const openEdit = (appointment: AppointmentSummary) => {
  emit('edit', appointment)
}

const openEditFromRow = (row: unknown) => {
  openEdit(row as AppointmentSummary)
}

const requestDelete = (appointment: AppointmentSummary) => {
  emit('delete', appointment)
}

const requestDeleteFromRow = (row: unknown) => {
  requestDelete(row as AppointmentSummary)
}
</script>

<template>
  <el-table
    class="appointment-board-table"
    :data="appointments"
    :loading="loading"
    border
    row-key="id"
    stripe
  >
    <!-- 表格字段与后端 AppointmentSummary 保持一致，分组表头仅负责展示。 -->
    <el-table-column fixed label="总序号" prop="globalSequence" width="82" />
    <el-table-column label="序号" prop="displaySequence" width="74" />
    <el-table-column label="所属公司" min-width="120" prop="companyName" show-overflow-tooltip />
    <el-table-column label="所属部门" min-width="120" prop="departmentName" show-overflow-tooltip />
    <el-table-column label="姓名" min-width="110">
      <template #default="{ row }">
        <el-button link type="primary" @click="openViewFromRow(row)">{{ row.name }}</el-button>
      </template>
    </el-table-column>
    <el-table-column label="现任职务" min-width="160" prop="currentPosition" show-overflow-tooltip />
    <el-table-column label="性别" prop="gender" width="76" />
    <el-table-column label="民族" min-width="90" prop="ethnicity" show-overflow-tooltip />
    <el-table-column label="身份证号" min-width="190" prop="idCard" show-overflow-tooltip />
    <el-table-column label="年龄" prop="age" width="76" />
    <el-table-column label="政治面貌" min-width="120" prop="politicalStatus" show-overflow-tooltip />
    <el-table-column label="全日制教育" align="center">
      <el-table-column label="学历" min-width="96" prop="fullTimeEducation" show-overflow-tooltip />
      <el-table-column label="学位" min-width="96" prop="fullTimeEducationDegree" show-overflow-tooltip />
      <el-table-column label="毕业院校" min-width="150" prop="fullTimeSchool" show-overflow-tooltip />
      <el-table-column label="专业" min-width="130" prop="fullTimeMajor" show-overflow-tooltip />
    </el-table-column>
    <el-table-column label="在职教育" align="center">
      <el-table-column label="学历" min-width="96" prop="partTimeEducation" show-overflow-tooltip />
      <el-table-column label="学位" min-width="96" prop="partTimeDegree" show-overflow-tooltip />
      <el-table-column label="毕业院校" min-width="150" prop="partTimeSchool" show-overflow-tooltip />
      <el-table-column label="专业" min-width="130" prop="partTimeMajor" show-overflow-tooltip />
    </el-table-column>
    <el-table-column label="专业技术职称" min-width="140" prop="technicalPosition" show-overflow-tooltip />
    <el-table-column label="联系方式（手机长号）" min-width="160" prop="phone" show-overflow-tooltip />
    <el-table-column label="婚姻状况" min-width="100" prop="maritalStatus" show-overflow-tooltip />
    <el-table-column label="备注" min-width="160" prop="remark" show-overflow-tooltip />
    <el-table-column fixed="right" label="操作" width="132">
      <template #default="{ row }">
        <el-button link type="primary" @click="openEditFromRow(row)">编辑</el-button>
        <el-button link type="danger" @click="requestDeleteFromRow(row)">删除</el-button>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty description="待录入任免记录" />
    </template>
  </el-table>
</template>

<style scoped>
.appointment-board-table {
  width: 100%;
}

.appointment-board-table :deep(.el-table__header th) {
  text-align: center;
}

.appointment-board-table :deep(.el-table__header .cell) {
  line-height: 1.35;
  white-space: normal;
  word-break: keep-all;
}
</style>
