<script setup lang="ts">
import { OfficeBuilding, Setting, UserFilled } from '@element-plus/icons-vue'
import { ElIcon, ElMenu, ElMenuItem, ElSubMenu } from 'element-plus'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { SystemMenu } from '@/api/system'

const SETTINGS_INDEX = '/settings'
const SETTINGS_USERS_PATH = '/settings/users'
const SETTINGS_LOGS_PATH = '/settings/logs'

const props = withDefaults(defineProps<{
  menus: SystemMenu[]
  collapsed?: boolean
}>(), {
  collapsed: false
})

const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)

const settingsChildren = [
  {
    name: '角色分配',
    path: SETTINGS_USERS_PATH
  },
  {
    name: '系统日志',
    path: SETTINGS_LOGS_PATH
  }
]

const renderMenus = computed(() => {
  let hasSettingsMenu = false

  return props.menus.flatMap((menu) => {
    if (menu.path !== SETTINGS_USERS_PATH) {
      return [
        {
          key: `menu-${menu.id}`,
          name: menu.name,
          path: menu.path,
          type: 'menu'
        }
      ]
    }

    // 后端只返回角色分配入口，前端固定补齐设置下的二级菜单。
    if (hasSettingsMenu) {
      return []
    }

    hasSettingsMenu = true

    return [
      {
        key: 'settings',
        name: '设置',
        path: SETTINGS_INDEX,
        type: 'settings'
      }
    ]
  })
})

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
    :collapse="collapsed"
    :default-active="activePath"
    :router="false"
    @select="handleSelect"
  >
    <template v-for="menu in renderMenus" :key="menu.key">
      <el-sub-menu
        v-if="menu.type === 'settings'"
        :index="menu.path"
        popper-class="permission-menu-popper"
      >
        <template #title>
          <el-icon>
            <component :is="menuIcon(SETTINGS_USERS_PATH)" />
          </el-icon>
          <span>{{ menu.name }}</span>
        </template>

        <el-menu-item v-for="child in settingsChildren" :key="child.path" :index="child.path">
          <span>{{ child.name }}</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item v-else :index="menu.path">
        <el-icon>
          <component :is="menuIcon(menu.path)" />
        </el-icon>
        <span>{{ menu.name }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>
