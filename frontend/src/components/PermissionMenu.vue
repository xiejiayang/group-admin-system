<script setup lang="ts">
import { OfficeBuilding, Setting, UserFilled } from '@element-plus/icons-vue'
import { ElIcon, ElMenu, ElMenuItem } from 'element-plus'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { SystemMenu } from '@/api/system'

defineProps<{
  menus: SystemMenu[]
}>()

const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)

const menuIcon = (path: string) => {
  if (path.startsWith('/settings')) {
    return Setting
  }

  if (path.startsWith('/general-admin')) {
    return OfficeBuilding
  }

  return UserFilled
}

const handleSelect = async (path: string) => {
  if (path !== route.path) {
    await router.push(path)
  }
}
</script>

<template>
  <el-menu
    class="permission-menu"
    :default-active="activePath"
    :router="false"
    @select="handleSelect"
  >
    <el-menu-item v-for="menu in menus" :key="menu.id" :index="menu.path">
      <el-icon>
        <component :is="menuIcon(menu.path)" />
      </el-icon>
      <span>{{ menu.name }}</span>
    </el-menu-item>
  </el-menu>
</template>
