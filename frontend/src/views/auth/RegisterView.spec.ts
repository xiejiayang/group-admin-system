import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import type { AuthUser } from '@/api/auth'

import RegisterView from './RegisterView.vue'

const registerMock = vi.hoisted(() => vi.fn())

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    register: registerMock
  })
}))

const createTestRouter = async () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: { template: '<div />' } },
      { path: '/register', component: RegisterView },
      { path: '/party-hr', component: { template: '<div />' } },
      { path: '/general-admin', component: { template: '<div />' } }
    ]
  })

  await router.push('/register')
  await router.isReady()

  return router
}

const registeredUser = (): AuthUser => ({
  userId: 2,
  username: 'new-user',
  realName: '张三',
  phone: '13800000000',
  departmentCode: 'PARTY_HR',
  departmentName: '党群人力部',
  roles: ['PARTY_HR_USER'],
  permissions: [],
  token: 'token'
})

describe('RegisterView', () => {
  beforeEach(() => {
    registerMock.mockReset()
  })

  it('renders the real name input', async () => {
    const router = await createTestRouter()

    const wrapper = mount(RegisterView, {
      global: {
        plugins: [ElementPlus, router]
      }
    })

    expect(wrapper.find('input[placeholder="请输入姓名"]').exists()).toBe(true)
  })

  it('passes trimmed realName when registering', async () => {
    const router = await createTestRouter()
    registerMock.mockResolvedValue(registeredUser())

    const wrapper = mount(RegisterView, {
      global: {
        plugins: [ElementPlus, router]
      }
    })

    await wrapper.find('input[autocomplete="username"]').setValue(' new-user ')
    await wrapper.find('input[autocomplete="name"]').setValue(' 张三 ')
    await wrapper.find('input[autocomplete="new-password"]').setValue('secret')
    await wrapper.find('input[autocomplete="tel"]').setValue('13800000000')
    await wrapper.find('button.auth-submit').trigger('click')
    await flushPromises()

    expect(registerMock).toHaveBeenCalledWith({
      username: 'new-user',
      realName: '张三',
      password: 'secret',
      phone: '13800000000',
      departmentCode: 'PARTY_HR'
    })
  })
})
