import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { SystemRole, SystemUser } from '@/api/system'

import RoleAssignDialog from './RoleAssignDialog.vue'

const fetchRolesMock = vi.hoisted(() => vi.fn())
const assignUserRolesMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/system', () => ({
  fetchRoles: fetchRolesMock,
  assignUserRoles: assignUserRolesMock
}))

enableAutoUnmount(afterEach)

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
  { id: 2, code: 'GENERAL_ADMIN_ADMIN', name: '综合管理部管理员' }
]

const deferred = <T>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })
  return { promise, resolve, reject }
}

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

  it('only submits current roles that are returned as assignable by backend', async () => {
    const user = {
      ...targetUser(),
      roles: ['DEPARTMENT_USER', 'GENERAL_ADMIN_USER']
    }
    assignUserRolesMock.mockResolvedValue({
      ...user,
      roles: ['GENERAL_ADMIN_USER']
    })
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

    const saveButton = Array.from(document.body.querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === '保存'
    )
    expect(saveButton).toBeTruthy()
    saveButton?.click()
    await flushPromises()

    expect(assignUserRolesMock).toHaveBeenCalledWith(user.id, {
      roleCodes: ['GENERAL_ADMIN_USER']
    })
  })

  it('ignores a stale role response after switching to another user', async () => {
    const userA = targetUser()
    const userB: SystemUser = {
      ...targetUser(),
      id: 8,
      username: 'party-user',
      departmentCode: 'PARTY_HR',
      departmentName: '党群人力部',
      roles: ['PARTY_HR_USER']
    }
    const requestA = deferred<SystemRole[]>()
    const requestB = deferred<SystemRole[]>()
    fetchRolesMock.mockImplementation((userId: number) => {
      return userId === userA.id ? requestA.promise : requestB.promise
    })
    assignUserRolesMock.mockResolvedValue(userB)
    const wrapper = mount(RoleAssignDialog, {
      props: {
        modelValue: false,
        user: userA
      },
      global: {
        plugins: [ElementPlus]
      },
      attachTo: document.body
    })

    await wrapper.setProps({ modelValue: true })
    await wrapper.setProps({ modelValue: false })
    await wrapper.setProps({ user: userB, modelValue: true })
    requestB.resolve([
      { id: 3, code: 'PARTY_HR_USER', name: '党群人力部用户' },
      { id: 4, code: 'PARTY_HR_ADMIN', name: '党群人力部管理员' }
    ])
    await flushPromises()

    expect(document.body.textContent).toContain('党群人力部用户')
    expect(document.body.textContent).not.toContain('综合管理员')

    requestA.resolve(roles())
    await flushPromises()

    expect(document.body.textContent).toContain('党群人力部用户')
    expect(document.body.textContent).not.toContain('综合管理员')
    expect(document.body.querySelector('.el-skeleton')).toBeNull()

    const saveButton = Array.from(document.body.querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === '保存'
    )
    saveButton?.click()
    await flushPromises()

    expect(assignUserRolesMock).toHaveBeenCalledWith(userB.id, {
      roleCodes: ['PARTY_HR_USER']
    })
  })

  it('ignores a stale role error after the current user roles have loaded', async () => {
    const userA = targetUser()
    const userB: SystemUser = {
      ...targetUser(),
      id: 8,
      username: 'party-user',
      departmentCode: 'PARTY_HR',
      departmentName: '党群人力部',
      roles: ['PARTY_HR_USER']
    }
    const requestA = deferred<SystemRole[]>()
    const requestB = deferred<SystemRole[]>()
    fetchRolesMock.mockImplementation((userId: number) => {
      return userId === userA.id ? requestA.promise : requestB.promise
    })
    const wrapper = mount(RoleAssignDialog, {
      props: {
        modelValue: false,
        user: userA
      },
      global: {
        plugins: [ElementPlus]
      },
      attachTo: document.body
    })

    await wrapper.setProps({ modelValue: true })
    await wrapper.setProps({ modelValue: false })
    await wrapper.setProps({ user: userB, modelValue: true })
    requestB.resolve([{ id: 3, code: 'PARTY_HR_USER', name: '党群人力部用户' }])
    await flushPromises()
    requestA.reject(new Error('用户 A 角色加载失败'))
    await flushPromises()

    expect(document.body.textContent).toContain('党群人力部用户')
    expect(document.body.textContent).not.toContain('用户 A 角色加载失败')
    expect(document.body.querySelector('.el-skeleton')).toBeNull()
  })
})
