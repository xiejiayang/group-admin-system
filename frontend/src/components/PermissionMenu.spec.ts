import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { SystemMenu } from '@/api/system'

import PermissionMenu from './PermissionMenu.vue'

const pushMock = vi.hoisted(() => vi.fn())
const routeState = vi.hoisted(() => ({ path: '/general-admin' }))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({
    push: pushMock
  })
}))

const menus = (): SystemMenu[] => [
  {
    id: 1,
    name: '综合管理',
    path: '/general-admin',
    permissionCode: 'general-admin:view',
    sortOrder: 1
  },
  {
    id: 2,
    name: '用户管理',
    path: '/settings/users',
    permissionCode: 'settings:users:view',
    sortOrder: 2
  }
]

describe('PermissionMenu', () => {
  beforeEach(() => {
    routeState.path = '/general-admin'
    pushMock.mockReset()
  })

  it('renders settings as a parent menu with fixed children', () => {
    const wrapper = mount(PermissionMenu, {
      props: {
        menus: menus()
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const text = wrapper.text()

    expect(text).toContain('设置')
    expect(text).toContain('角色分配')
    expect(text).toContain('系统日志')
  })

  it('pushes the system logs path when the logs child menu is selected', async () => {
    const wrapper = mount(PermissionMenu, {
      props: {
        menus: menus()
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const logsItem = wrapper
      .findAllComponents({ name: 'ElMenuItem' })
      .find((item) => item.props('index') === '/settings/logs')

    expect(logsItem?.text()).toContain('系统日志')

    await logsItem?.trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/settings/logs')
  })
})
