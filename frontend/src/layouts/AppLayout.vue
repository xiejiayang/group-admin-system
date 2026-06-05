<script setup lang="ts">
import { ArrowLeft, ArrowRight, Refresh, SwitchButton, User } from '@element-plus/icons-vue'
import {
  ElAlert,
  ElAside,
  ElButton,
  ElContainer,
  ElEmpty,
  ElHeader,
  ElIcon,
  ElMain,
  ElSkeleton,
  ElTag
} from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { RouterView, useRouter } from 'vue-router'

import PermissionMenu from '@/components/PermissionMenu.vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const sidebarCollapsed = ref(false)

const user = computed(() => authStore.user)
const departmentText = computed(() => user.value?.departmentName || user.value?.departmentCode || '集团后台')
const roleText = computed(() => user.value?.roles.join('、') || '未分配角色')
const sidebarWidth = computed(() => (sidebarCollapsed.value ? '72px' : '224px'))
const sidebarToggleIcon = computed(() => (sidebarCollapsed.value ? ArrowRight : ArrowLeft))

const loadMenus = async (force = false) => {
  try {
    await menuStore.loadMenus(force)
  } catch {
    // 菜单错误由 store.error 统一展示，这里只避免布局挂载时出现未处理异常。
  }
}

onMounted(() => {
  void loadMenus()
})

const handleRetryMenus = () => {
  void loadMenus(true)
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleLogout = async () => {
  authStore.logout()
  menuStore.clear()
  await router.push('/login')
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside class="app-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }" :width="sidebarWidth">
      <button
        class="sidebar-toggle"
        type="button"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="toggleSidebar"
      >
        <el-icon>
          <component :is="sidebarToggleIcon" />
        </el-icon>
      </button>

      <div class="app-brand">
        <span class="app-brand-mark">管</span>
        <div class="app-brand-text">
          <strong>集团后台</strong>
          <small>管理系统</small>
        </div>
      </div>

      <el-skeleton v-if="menuStore.loading && menuStore.menus.length === 0" animated :rows="4" />
      <div v-else-if="menuStore.error" class="menu-state">
        <el-alert class="menu-error-alert" :closable="false" :title="menuStore.error" show-icon type="error" />
        <el-button :icon="Refresh" :loading="menuStore.loading" plain @click="handleRetryMenus">重试</el-button>
      </div>
      <div v-else-if="menuStore.menus.length === 0" class="menu-state menu-empty-state">
        <el-empty description="暂无可访问菜单" :image-size="64" />
      </div>
      <permission-menu v-else :collapsed="sidebarCollapsed" :menus="menuStore.menus" />
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
