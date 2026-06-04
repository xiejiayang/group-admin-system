import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { SystemRole, SystemUser } from '@/api/system'

import RoleAssignDialog from './RoleAssignDialog.vue'

const fetchRolesMock = vi.hoisted(() => vi.fn())
const assignUserRolesMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/system', () => ({
  fetchRoles: fetchRolesMock,
  assignUserRoles: assignUserRolesMock
}))

const targetUser = (): SystemUser => ({
  id: 7,
  username: 'operator',
  realName: '王五',
  phone: '13900000000',
  departmentCode: 'GENERAL_ADMIN',
  departmentName: '综合管理部',
  roles: ['GENERAL_ADMIN_USER'],
  status: 'ENABLED'
})

const roles = (): SystemRole[] => [
  { id: 1, code: 'GENERAL_ADMIN_USER', name: '综合管理员' },
  { id: 2, code: 'SUPER_ADMIN', name: '超级管理员' }
]

describe('RoleAssignDialog', () => {
  beforeEach(() => {
    fetchRolesMock.mockReset()
    assignUserRolesMock.mockReset()
    fetchRolesMock.mockResolvedValue(roles())
  })

  it('fetches roles with targetUserId when opened', async () => {
    const user = targetUser()
    const wrapper = mount(RoleAssignDialog, {
      props: {
        modelValue: false,
        user
      },
      global: {
        plugins: [ElementPlus]
      },
      attachTo: document.body
    })

    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    expect(fetchRolesMock).toHaveBeenCalledWith(user.id)
  })

  it('does not fetch roles when opened without a user', async () => {
    const wrapper = mount(RoleAssignDialog, {
      props: {
        modelValue: false,
        user: null
      },
      global: {
        plugins: [ElementPlus]
      },
      attachTo: document.body
    })

    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    expect(fetchRolesMock).not.toHaveBeenCalled()
  })
})
