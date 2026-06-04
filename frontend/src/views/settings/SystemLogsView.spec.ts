import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import SystemLogsView from './SystemLogsView.vue'

const fetchOperationLogsMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/system', () => ({
  fetchOperationLogs: fetchOperationLogsMock
}))

describe('SystemLogsView', () => {
  beforeEach(() => {
    fetchOperationLogsMock.mockReset()
  })

  const operationLog = () => ({
    id: 1,
    operatorUsername: 'admin',
    realName: '管理员',
    department: null,
    phone: '13800000000',
    role: 'SUPER_ADMIN',
    operationRecord: '分配用户角色'
  })

  it('renders the operation log title and table headers', async () => {
    fetchOperationLogsMock.mockResolvedValue([operationLog()])

    const wrapper = mount(SystemLogsView, {
      global: {
        plugins: [ElementPlus]
      }
    })

    await flushPromises()

    const text = wrapper.text()

    expect(text).toContain('操作记录')
    expect(text).toContain('操作账号')
    expect(text).toContain('姓名')
    expect(text).toContain('部门')
    expect(text).toContain('手机号')
    expect(text).toContain('角色')
  })

  it('loads and renders operation log rows', async () => {
    fetchOperationLogsMock.mockResolvedValue([operationLog()])

    const wrapper = mount(SystemLogsView, {
      global: {
        plugins: [ElementPlus]
      },
      attachTo: document.body
    })

    await flushPromises()

    const text = wrapper.text()

    expect(fetchOperationLogsMock).toHaveBeenCalledTimes(1)
    expect(text).toContain('admin')
    expect(text).toContain('分配用户角色')
  })
})
