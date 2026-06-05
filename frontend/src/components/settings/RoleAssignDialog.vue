<script setup lang="ts">
import {
  ElAlert,
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElDialog,
  ElEmpty,
  ElMessage,
  ElSkeleton,
  ElTag
} from 'element-plus'
import { computed, ref, watch } from 'vue'

import { assignUserRoles, fetchRoles, type SystemRole, type SystemUser } from '@/api/system'

const props = defineProps<{
  modelValue: boolean
  user: SystemUser | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [user: SystemUser]
}>()

const roles = ref<SystemRole[]>([])
const selectedRoleCodes = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)
const roleLoadError = ref('')
const rolesLoaded = ref(false)
let roleRequestSequence = 0

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => {
    if (!value && saving.value) {
      return
    }

    emit('update:modelValue', value)
  }
})

const canSave = computed(() => {
  return rolesLoaded.value && !roleLoadError.value && selectedRoleCodes.value.length > 0 && !loading.value && !saving.value
})

const toMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message : fallback
}

const loadRoles = async () => {
  const requestSequence = ++roleRequestSequence
  const targetUser = props.user
  roles.value = []
  rolesLoaded.value = false
  roleLoadError.value = ''
  loading.value = false

  if (!targetUser) {
    return
  }

  loading.value = true
  const isCurrentRequest = () => {
    return (
      requestSequence === roleRequestSequence &&
      props.modelValue &&
      props.user?.id === targetUser.id
    )
  }

  try {
    // 角色可选范围取决于“被分配人”，必须把目标用户 ID 传给后端统一裁剪。
    const loadedRoles = await fetchRoles(targetUser.id)
    if (!isCurrentRequest()) {
      return
    }
    roles.value = loadedRoles
    const assignableRoleCodes = new Set(loadedRoles.map((role) => role.code))
    // 仅回显后端允许分配的现有角色，避免历史隐藏角色被再次提交。
    selectedRoleCodes.value = targetUser.roles.filter((roleCode) => assignableRoleCodes.has(roleCode))
    rolesLoaded.value = true
  } catch (error) {
    if (!isCurrentRequest()) {
      return
    }
    const message = toMessage(error, '角色列表加载失败')
    roleLoadError.value = message
    ElMessage.error(message)
  } finally {
    if (isCurrentRequest()) {
      loading.value = false
    }
  }
}

watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) {
      roleRequestSequence += 1
      return
    }

    selectedRoleCodes.value = [...(props.user?.roles ?? [])]
    void loadRoles()
  }
)

const handleRetryRoles = () => {
  void loadRoles()
}

const handleBeforeClose = (done: () => void) => {
  if (saving.value) {
    return
  }

  done()
}

const handleClose = () => {
  if (saving.value) {
    return
  }

  visible.value = false
}

const handleSave = async () => {
  if (!props.user) {
    return
  }

  if (!canSave.value) {
    ElMessage.warning(roleLoadError.value || '请至少选择一个角色')
    return
  }

  saving.value = true

  try {
    const updatedUser = await assignUserRoles(props.user.id, { roleCodes: selectedRoleCodes.value })
    ElMessage.success('角色已保存')
    emit('saved', updatedUser)
    emit('update:modelValue', false)
  } catch (error) {
    const message = toMessage(error, '角色保存失败')
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :before-close="handleBeforeClose"
    class="role-dialog"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    :show-close="!saving"
    title="分配角色"
    width="min(520px, calc(100vw - 32px))"
  >
    <div v-if="user" class="role-dialog-user">
      <span>{{ user.username }}</span>
      <el-tag size="small" type="info">{{ user.departmentName || user.departmentCode || '未分配部门' }}</el-tag>
    </div>

    <el-skeleton v-if="loading" animated :rows="4" />
    <div v-else-if="roleLoadError" class="role-load-error">
      <el-alert :closable="false" :title="roleLoadError" show-icon type="error" />
      <el-button :loading="loading" plain @click="handleRetryRoles">重试</el-button>
    </div>
    <el-empty v-else-if="rolesLoaded && roles.length === 0" description="暂无可分配角色" />
    <el-checkbox-group v-else v-model="selectedRoleCodes" class="role-option-list">
      <el-checkbox v-for="role in roles" :key="role.id" :value="role.code" border>
        <span class="role-option-name">{{ role.name }}</span>
        <small>{{ role.code }}</small>
      </el-checkbox>
    </el-checkbox-group>

    <template #footer>
      <el-button :disabled="saving" @click="handleClose">取消</el-button>
      <el-button type="primary" :disabled="!canSave" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>
