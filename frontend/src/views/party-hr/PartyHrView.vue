<script setup lang="ts">
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElButton, ElCard, ElConfigProvider, ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { onMounted, ref } from 'vue'

import { deleteAppointment, fetchAppointments } from '@/api/appointment'
import AppointmentFormDialog from '@/components/appointment/AppointmentFormDialog.vue'
import AppointmentTable from '@/components/appointment/AppointmentTable.vue'
import type { AppointmentFormMode, AppointmentSummary } from '@/types/appointment'

const appointments = ref<AppointmentSummary[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<AppointmentFormMode>('view')
const selectedAppointmentId = ref<number | null>(null)

const paginationLocale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    pagination: {
      ...zhCn.el.pagination,
      total: '总计 {total} 条',
      pagesize: '条/页'
    }
  }
}

const toMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message : fallback
}

const loadAppointments = async () => {
  loading.value = true

  try {
    const result = await fetchAppointments({ page: page.value, size: size.value })
    appointments.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(toMessage(error, '任免记录加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadAppointments()
})

const openCreateDialog = () => {
  selectedAppointmentId.value = null
  dialogMode.value = 'create'
  dialogVisible.value = true
}

const openViewDialog = (appointment: AppointmentSummary) => {
  selectedAppointmentId.value = appointment.id
  dialogMode.value = 'view'
  dialogVisible.value = true
}

const openEditDialog = (appointment: AppointmentSummary) => {
  selectedAppointmentId.value = appointment.id
  dialogMode.value = 'edit'
  dialogVisible.value = true
}

const resetDialog = () => {
  selectedAppointmentId.value = null
}

const handleCurrentChange = (currentPage: number) => {
  page.value = currentPage - 1
  void loadAppointments()
}

const handleSizeChange = (pageSize: number) => {
  size.value = pageSize
  page.value = 0
  void loadAppointments()
}

const handleDelete = async (appointment: AppointmentSummary) => {
  try {
    await ElMessageBox.confirm(`确认删除“${appointment.name}”的任免记录吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteAppointment(appointment.id)
    ElMessage.success('任免记录已删除')

    if (appointments.value.length === 1 && page.value > 0) {
      page.value -= 1
    }

    await loadAppointments()
  } catch (error) {
    ElMessage.error(toMessage(error, '任免记录删除失败'))
  }
}
</script>

<template>
  <section class="party-hr-page">
    <div class="page-heading">
      <div>
        <h1>任免看板</h1>
        <p>维护任免记录，查看或编辑任免审批表。</p>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadAppointments">刷新</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增</el-button>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <appointment-table
        :appointments="appointments"
        :loading="loading"
        @delete="handleDelete"
        @edit="openEditDialog"
        @view="openViewDialog"
      />

      <div v-if="total > 0" class="appointment-pagination">
        <el-config-provider :locale="paginationLocale">
          <el-pagination
            background
            :current-page="page + 1"
            layout="total, sizes, prev, pager, next"
            :page-size="size"
            :page-sizes="[10, 20, 50]"
            :total="total"
            @current-change="handleCurrentChange"
            @size-change="handleSizeChange"
          />
        </el-config-provider>
      </div>
    </el-card>

    <appointment-form-dialog
      v-model="dialogVisible"
      :appointment-id="selectedAppointmentId"
      :mode="dialogMode"
      @close="resetDialog"
      @refresh="loadAppointments"
    />
  </section>
</template>

<style scoped>
.party-hr-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.appointment-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 720px) {
  .page-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .appointment-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
