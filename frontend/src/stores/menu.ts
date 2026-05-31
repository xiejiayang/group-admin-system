import { defineStore } from 'pinia'

import { fetchMenus, type SystemMenu } from '@/api/system'

interface MenuState {
  menus: SystemMenu[]
  loading: boolean
  loaded: boolean
  error: string
}

let inFlightMenus: Promise<SystemMenu[]> | null = null

const toErrorMessage = (error: unknown) => {
  return error instanceof Error ? error.message : '菜单加载失败'
}

export const useMenuStore = defineStore('menu', {
  state: (): MenuState => ({
    menus: [],
    loading: false,
    loaded: false,
    error: ''
  }),
  actions: {
    async loadMenus(force = false) {
      if (this.loaded && !force) {
        return this.menus
      }

      if (inFlightMenus) {
        return inFlightMenus
      }

      if (force) {
        this.menus = []
        this.loaded = false
      }

      this.loading = true
      this.error = ''

      inFlightMenus = fetchMenus()
        .then((menus) => {
          // 菜单必须以后端返回为准，前端仅排序兜底，避免显示用户无权访问的入口。
          this.menus = [...menus].sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
          this.loaded = true
          this.error = ''
          return this.menus
        })
        .catch((error) => {
          this.menus = []
          this.loaded = false
          this.error = toErrorMessage(error)
          throw error
        })
        .finally(() => {
          this.loading = false
          inFlightMenus = null
        })

      return inFlightMenus
    },
    clear() {
      this.menus = []
      this.loading = false
      this.loaded = false
      this.error = ''
      inFlightMenus = null
    }
  }
})
