<script setup lang="ts">
import { SwitchButton, User } from '@element-plus/icons-vue'
import {
  ElAside,
  ElButton,
  ElContainer,
  ElHeader,
  ElIcon,
  ElMain,
  ElSkeleton,
  ElTag
} from 'element-plus'
import { computed, onMounted } from 'vue'
import { RouterView, useRouter } from 'vue-router'

import PermissionMenu from '@/components/PermissionMenu.vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

const user = computed(() => authStore.user)
const departmentText = computed(() => user.value?.departmentName || user.value?.departmentCode || '集团后台')
const roleText = computed(() => user.value?.roles.join('、') || '未分配角色')

onMounted(() => {
  void menuStore.loadMenus()
})

const handleLogout = async () => {
  authStore.logout()
  menuStore.clear()
  await router.push('/login')
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside class="app-sidebar" width="224px">
      <div class="app-brand">
        <span class="app-brand-mark">管</span>
        <div>
          <strong>集团后台</strong>
          <small>管理系统</small>
        </div>
      </div>

      <el-skeleton v-if="menuStore.loading && menuStore.menus.length === 0" animated :rows="4" />
      <permission-menu v-else :menus="menuStore.menus" />
    </el-aside>

    <el-container class="app-main-shell">
      <el-header class="app-header" height="64px">
        <div class="app-header-title">
          <span>后台管理</span>
          <el-tag size="small" type="info">{{ departmentText }}</el-tag>
        </div>

        <div class="app-user">
          <el-icon><User /></el-icon>
          <div class="app-user-text">
            <span>{{ user?.username || '未登录用户' }}</span>
            <small>{{ roleText }}</small>
          </div>
          <el-button :icon="SwitchButton" type="primary" plain @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="app-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
