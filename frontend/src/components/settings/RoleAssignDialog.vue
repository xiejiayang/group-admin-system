<script setup lang="ts">
import {
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElDialog,
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
  saved: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const roles = ref<SystemRole[]>([])
const selectedRoleCodes = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)

const loadRoles = async () => {
  loading.value = true

  try {
    roles.value = await fetchRoles()
  } catch (error) {
    const message = error instanceof Error ? error.message : '角色列表加载失败'
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

    selectedRoleCodes.value = [...(props.user?.roles ?? [])]
    void loadRoles()
  }
)

const handleClose = () => {
  visible.value = false
}

const handleSave = async () => {
  if (!props.user) {
    return
  }

  saving.value = true

  try {
    // 角色保存只提交角色 code 列表，保存成功后通知父页面刷新用户表格。
    await assignUserRoles(props.user.id, { roleCodes: selectedRoleCodes.value })
    ElMessage.success('角色已保存')
    emit('saved')
    visible.value = false
  } catch (error) {
    const message = error instanceof Error ? error.message : '角色保存失败'
    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" class="role-dialog" title="分配角色" width="520px" @close="handleClose">
    <div v-if="user" class="role-dialog-user">
      <span>{{ user.username }}</span>
      <el-tag size="small" type="info">{{ user.departmentName || user.departmentCode || '未分配部门' }}</el-tag>
    </div>

    <el-skeleton v-if="loading" animated :rows="4" />
    <el-checkbox-group v-else v-model="selectedRoleCodes" class="role-option-list">
      <el-checkbox v-for="role in roles" :key="role.id" :value="role.code" border>
        <span class="role-option-name">{{ role.name }}</span>
        <small>{{ role.code }}</small>
      </el-checkbox>
    </el-checkbox-group>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>
