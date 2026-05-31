import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import LoginView from './LoginView.vue'

describe('LoginView', () => {
  it('shows login and register entry', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: LoginView },
        { path: '/register', component: { template: '<div />' } }
      ]
    })
    await router.push('/login')
    await router.isReady()

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
})
