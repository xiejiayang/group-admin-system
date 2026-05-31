<script setup lang="ts">
import { Refresh, UserFilled } from '@element-plus/icons-vue'
import {
  ElButton,
  ElCard,
  ElEmpty,
  ElMessage,
  ElSkeleton,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchUsers, type SystemUser } from '@/api/system'
import RoleAssignDialog from '@/components/settings/RoleAssignDialog.vue'
import { accessibleLandingPath, canAccessPath } from '@/router/access'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

const users = ref<SystemUser[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const selectedUser = ref<SystemUser | null>(null)

const hasUsers = computed(() => users.value.length > 0)

const toMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message : fallback
}

const loadUsers = async () => {
  loading.value = true

  try {
    users.value = await fetchUsers()
  } catch (error) {
    const message = toMessage(error, '用户列表加载失败')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadUsers()
})

const statusText = (status: string) => {
  return status === 'ENABLED' ? '启用' : '停用'
}

const openRoleDialog = (user: SystemUser) => {
  selectedUser.value = user
  dialogVisible.value = true
}

const openRoleDialogFromRow = (row: unknown) => {
  openRoleDialog(row as SystemUser)
}

const replaceUser = (updatedUser: SystemUser) => {
  users.value = users.value.map((user) => (user.id === updatedUser.id ? updatedUser : user))
}

const syncCurrentSessionAfterRoleChange = async () => {
  // 当前账号角色变化后必须刷新菜单和权限，避免页面可进但接口返回 403。
  const currentUser = await authStore.loadCurrentUser()
  const menus = await menuStore.loadMenus(true)
  const roles = currentUser?.roles ?? []
  const permissions = currentUser?.permissions ?? []
  const departmentCode = currentUser?.departmentCode
  const landingPath = accessibleLandingPath(roles, permissions, departmentCode, menus)

  if (!canAccessPath(router.currentRoute.value.path, roles, permissions, departmentCode)) {
    await router.replace(landingPath)
    return false
  }

  return true
}

const handleRolesSaved = async (updatedUser: SystemUser) => {
  replaceUser(updatedUser)

  if (authStore.user?.userId !== updatedUser.id) {
    await loadUsers()
    return
  }

  try {
    const canStayOnCurrentPage = await syncCurrentSessionAfterRoleChange()
    if (canStayOnCurrentPage) {
      await loadUsers()
    }
  } catch (error) {
    const message = toMessage(error, '当前账号权限刷新失败，请重新登录')
    ElMessage.error(message)
    if (!authStore.token) {
      await router.replace('/login')
    }
  }
}
</script>

<template>
  <section class="settings-page">
    <div class="page-heading">
      <div>
        <h1>用户角色分配</h1>
        <p>维护账号所属角色，权限变更保存后即时刷新列表。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadUsers">刷新</el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <el-skeleton v-if="loading && !hasUsers" class="table-skeleton" animated :rows="6" />
      <el-table v-else-if="hasUsers" :data="users" row-key="id">
        <el-table-column label="账号" min-width="150" prop="username" show-overflow-tooltip />
        <el-table-column label="手机号" min-width="140" prop="phone" show-overflow-tooltip />
        <el-table-column label="部门" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.departmentName || row.departmentCode || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <div class="role-tags">
              <el-tag v-for="role in row.roles" :key="role" size="small">{{ role }}</el-tag>
              <span v-if="row.roles.length === 0" class="muted-text">未分配</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="130">
          <template #default="{ row }">
            <el-button :icon="UserFilled" size="small" type="primary" @click="openRoleDialogFromRow(row)">
              分配角色
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="暂无用户数据" />
    </el-card>

    <role-assign-dialog v-model="dialogVisible" :user="selectedUser" @saved="handleRolesSaved" />
  </section>
</template>
