import { defineStore } from 'pinia'

import { fetchMenus, type SystemMenu } from '@/api/system'

interface MenuState {
  menus: SystemMenu[]
  loading: boolean
  loaded: boolean
}

export const useMenuStore = defineStore('menu', {
  state: (): MenuState => ({
    menus: [],
    loading: false,
    loaded: false
  }),
  actions: {
    async loadMenus(force = false) {
      if (this.loaded && !force) {
        return this.menus
      }

      this.loading = true

      try {
        // 菜单加载以服务端返回为准，前端只做排序兜底，避免展示用户无权访问的入口。
        const menus = await fetchMenus()
        this.menus = [...menus].sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
        this.loaded = true
        return this.menus
      } finally {
        this.loading = false
      }
    },
    clear() {
      this.menus = []
      this.loading = false
      this.loaded = false
    }
  }
})
