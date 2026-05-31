import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import type { AuthUser } from '@/api/auth'

import LoginView from './LoginView.vue'

const loginMock = vi.hoisted(() => vi.fn())

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    login: loginMock
  })
}))

const createTestRouter = async (initialPath = '/login') => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/register', component: { template: '<div />' } },
      { path: '/party-hr', component: { template: '<div />' } },
      { path: '/general-admin', component: { template: '<div />' } }
    ]
  })

  await router.push(initialPath)
  await router.isReady()

  return router
}

const generalAdminUser = (): AuthUser => ({
  userId: 1,
  username: 'admin',
  phone: '13800000000',
  departmentCode: 'GENERAL_ADMIN',
  departmentName: '综合管理部',
  roles: ['GENERAL_ADMIN'],
  permissions: [],
  token: 'token'
})

describe('LoginView', () => {
  beforeEach(() => {
    loginMock.mockReset()
  })

  it('shows login and register entry', async () => {
    const router = await createTestRouter()

    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, router]
      }
    })

    expect(wrapper.text()).toContain('集团后台管理系统')
    expect(wrapper.text()).toContain('登录')
    expect(wrapper.text()).toContain('注册账号')
    expect(wrapper.find('input[placeholder="请输入账号"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="请输入密码"]').exists()).toBe(true)
  })

  it('redirects to the internal redirect query after login', async () => {
    const redirectPath = '/party-hr?preview=1'
    const router = await createTestRouter(`/login?redirect=${encodeURIComponent(redirectPath)}`)
    loginMock.mockResolvedValue(generalAdminUser())

    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, router]
      }
    })

    await wrapper.find('input[autocomplete="username"]').setValue(' admin ')
    await wrapper.find('input[autocomplete="current-password"]').setValue('secret')
    await wrapper.find('button.auth-submit').trigger('click')
    await flushPromises()

    expect(loginMock).toHaveBeenCalledWith({ username: 'admin', password: 'secret' })
    expect(router.currentRoute.value.fullPath).toBe(redirectPath)
  })

  it('falls back to the role landing path for external redirect query', async () => {
    const router = await createTestRouter(
      `/login?redirect=${encodeURIComponent('https://evil.example/admin')}`
    )
    loginMock.mockResolvedValue(generalAdminUser())

    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, router]
      }
    })

    await wrapper.find('input[autocomplete="username"]').setValue('admin')
    await wrapper.find('input[autocomplete="current-password"]').setValue('secret')
    await wrapper.find('button.auth-submit').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/general-admin')
  })
})
