<script setup lang="ts">
import { ElAlert, ElButton, ElDialog, ElMessage, ElSkeleton } from 'element-plus'
import { computed, ref, watch } from 'vue'

import { createAppointment, fetchAppointmentDetail, updateAppointment } from '@/api/appointment'
import AppointmentFormGrid from '@/components/appointment/AppointmentFormGrid.vue'
import {
  createEmptyAppointmentForm,
  normalizeAppointmentForm,
  sanitizeAppointmentPayload,
  type AppointmentFormMode,
  type AppointmentFormPayload
} from '@/types/appointment'

const props = defineProps<{
  modelValue: boolean
  mode: AppointmentFormMode
  appointmentId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
  close: []
}>()

const form = ref<AppointmentFormPayload>(createEmptyAppointmentForm())
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => {
    if (!value && saving.value) {
      return
    }

    emit('update:modelValue', value)
  }
})

const title = computed(() => {
  if (props.mode === 'create') {
    return '新增任免审批表'
  }

  if (props.mode === 'edit') {
    return '编辑任免审批表'
  }

  return '查看任免审批表'
})

const canSave = computed(() => props.mode !== 'view' && !loading.value && !saving.value && !loadError.value)

const toMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message : fallback
}

const loadDetail = async (id: number) => {
  loading.value = true
  loadError.value = ''

  try {
    form.value = normalizeAppointmentForm(await fetchAppointmentDetail(id))
  } catch (error) {
    const message = toMessage(error, '任免审批表加载失败')
    loadError.value = message
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) {
      return
    }

    loadError.value = ''

    if (props.mode === 'create') {
      form.value = createEmptyAppointmentForm()
      return
    }

    if (props.appointmentId) {
      void loadDetail(props.appointmentId)
    }
  }
)

const handleBeforeClose = (done: () => void) => {
  if (!saving.value) {
    done()
  }
}

const handleClose = () => {
  visible.value = false
}

const validateRequiredFields = (payload: AppointmentFormPayload) => {
  // 保存校验只检查当前审批表必填字段，避免旧看板字段阻断提交。
  const requiredValues = [
    payload.name,
    payload.companyName,
    payload.departmentName,
    payload.currentPosition,
    payload.phone
  ]

  return requiredValues.every((value) => value.trim().length > 0)
}

const handleSave = async () => {
  if (props.mode === 'view') {
    return
  }

  const payload = sanitizeAppointmentPayload(form.value)

  if (!validateRequiredFields(payload)) {
    ElMessage.warning('请填写姓名、所属公司、所属部门、现任职务和联系方式（手机长号）')
    return
  }

  saving.value = true

  try {
    if (props.mode === 'create') {
      await createAppointment(payload)
    } else if (props.appointmentId) {
      await updateAppointment(props.appointmentId, payload)
    }

    ElMessage.success('任免审批表已保存')
    emit('refresh')
    visible.value = false
  } catch (error) {
    ElMessage.error(toMessage(error, '任免审批表保存失败'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :before-close="handleBeforeClose"
    class="appointment-dialog"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    destroy-on-close
    :show-close="!saving"
    :title="title"
    top="4vh"
    width="min(1180px, calc(100vw - 32px))"
    @closed="emit('close')"
  >
    <el-skeleton v-if="loading" animated :rows="10" />
    <el-alert v-else-if="loadError" :closable="false" :title="loadError" show-icon type="error" />
    <appointment-form-grid v-else v-model="form" :mode="mode" />

    <template #footer>
      <el-button :disabled="saving" @click="handleClose">{{ mode === 'view' ? '关闭' : '取消' }}</el-button>
      <el-button v-if="mode !== 'view'" :disabled="!canSave" :loading="saving" type="primary" @click="handleSave">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.appointment-dialog :deep(.el-dialog__body) {
  max-height: calc(92vh - 136px);
  overflow: auto;
  padding: 18px 20px;
  background: #f8fafc;
}

.appointment-dialog :deep(.el-dialog__header),
.appointment-dialog :deep(.el-dialog__footer) {
  padding-right: 20px;
  padding-left: 20px;
}
</style>
