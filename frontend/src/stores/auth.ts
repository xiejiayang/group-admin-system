import { defineStore } from 'pinia'

import {
  fetchCurrentUser,
  login as loginRequest,
  register as registerRequest,
  type AuthUser,
  type LoginRequest,
  type RegisterRequest
} from '@/api/auth'
import { TOKEN_STORAGE_KEY } from '@/api/http'

interface AuthState {
  token: string
  user: AuthUser | null
}

const storedToken = () => {
  return localStorage.getItem(TOKEN_STORAGE_KEY) ?? ''
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: storedToken(),
    user: null
  }),
  actions: {
    setSession(user: AuthUser) {
      this.user = user
      this.token = user.token

      // 登录或注册成功后同步本地 token，刷新页面时请求拦截器仍可携带认证信息。
      localStorage.setItem(TOKEN_STORAGE_KEY, user.token)
    },
    async login(payload: LoginRequest) {
      const user = await loginRequest(payload)
      this.setSession(user)
      return user
    },
    async register(payload: RegisterRequest) {
      const user = await registerRequest(payload)
      this.setSession(user)
      return user
    },
    async loadCurrentUser() {
      if (!this.token) {
        return null
      }

      try {
        const user = await fetchCurrentUser()
        this.setSession(user)
        return user
      } catch (error) {
        // 当前 token 无效时清理认证态，避免后续请求继续携带失效凭证。
        this.logout()
        throw error
      }
    },
    logout() {
      this.token = ''
      this.user = null

      // 退出登录必须同时清理持久化 token，保证刷新后仍处于未登录状态。
      localStorage.removeItem(TOKEN_STORAGE_KEY)
    }
  }
})
