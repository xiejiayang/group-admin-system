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
    v-if="appointments.length > 0 || loading"
    :data="appointments"
    :loading="loading"
    row-key="id"
    stripe
  >
    <el-table-column fixed label="姓名" min-width="120">
      <template #default="{ row }">
        <el-button link type="primary" @click="openViewFromRow(row)">{{ row.name }}</el-button>
      </template>
    </el-table-column>
    <el-table-column label="电话" min-width="140" prop="phone" show-overflow-tooltip />
    <el-table-column label="身份证号" min-width="190" prop="idCard" show-overflow-tooltip />
    <el-table-column label="职位" min-width="150" prop="positionName" show-overflow-tooltip />
    <el-table-column label="毕业院校" min-width="180" prop="graduationSchool" show-overflow-tooltip />
    <el-table-column label="地址" min-width="220" prop="address" show-overflow-tooltip />
    <el-table-column fixed="right" label="操作" width="132">
      <template #default="{ row }">
        <el-button link type="primary" @click="openEditFromRow(row)">编辑</el-button>
        <el-button link type="danger" @click="requestDeleteFromRow(row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <el-empty v-else description="待录入任免记录" />
</template>
