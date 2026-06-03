import { mount } from '@vue/test-utils'
import ElementPlus, { ElButton, ElTableColumn } from 'element-plus'
import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'

import type { AppointmentSummary } from '@/types/appointment'

import AppointmentTable from './AppointmentTable.vue'

const sampleAppointment: AppointmentSummary = {
  id: 7,
  globalSequence: 101,
  displaySequence: 3,
  companyName: '集团公司',
  departmentName: '党群人力部',
  name: '张三',
  currentPosition: '党群人力部部长',
  gender: '男',
  ethnicity: '汉族',
  idCard: '110101199001011234',
  age: 36,
  politicalStatus: '中共党员',
  fullTimeEducation: '本科',
  fullTimeEducationDegree: '学士',
  fullTimeSchool: '清华大学',
  fullTimeMajor: '法学',
  partTimeEducation: '研究生',
  partTimeDegree: '硕士',
  partTimeSchool: '北京大学',
  partTimeMajor: '工商管理',
  technicalPosition: '高级经济师',
  phone: '13800000000',
  maritalStatus: '已婚',
  remark: '重点培养'
}

const mountTable = async () => {
  const wrapper = mount(AppointmentTable, {
    props: {
      appointments: [sampleAppointment]
    },
    global: {
      plugins: [ElementPlus]
    },
    attachTo: document.body
  })

  await nextTick()
  await nextTick()

  return wrapper
}

describe('AppointmentTable', () => {
  it('renders appointment board headers with grouped education columns and remark column', async () => {
    const wrapper = await mountTable()
    const text = wrapper.text()
    const columnLabels = wrapper.findAllComponents(ElTableColumn).map((column) => column.props('label'))

    expect(text).toContain('全日制教育')
    expect(text).toContain('在职教育')
    expect(text).toContain('备注')
    expect(columnLabels).toEqual([
      '总序号',
      '序号',
      '所属公司',
      '所属部门',
      '姓名',
      '现任职务',
      '性别',
      '民族',
      '身份证号',
      '年龄',
      '政治面貌',
      '全日制教育',
      '学历',
      '学位',
      '毕业院校',
      '专业',
      '在职教育',
      '学历',
      '学位',
      '毕业院校',
      '专业',
      '专业技术职称',
      '联系方式（手机长号）',
      '婚姻状况',
      '备注',
      '操作'
    ])
  })

  it('displays summary values for the appointment board columns', async () => {
    const wrapper = await mountTable()
    const text = wrapper.text()

    expect(text).toContain('101')
    expect(text).toContain('3')
    expect(text).toContain('集团公司')
    expect(text).toContain('党群人力部部长')
    expect(text).toContain('13800000000')
    expect(text).toContain('重点培养')
  })

  it('emits view, edit, and delete events from table buttons', async () => {
    const wrapper = await mountTable()
    const buttons = wrapper.findAllComponents(ElButton)

    await buttons.find((button) => button.text() === '张三')?.trigger('click')
    await buttons.find((button) => button.text() === '编辑')?.trigger('click')
    await buttons.find((button) => button.text() === '删除')?.trigger('click')

    expect(wrapper.emitted('view')?.[0]).toEqual([sampleAppointment])
    expect(wrapper.emitted('edit')?.[0]).toEqual([sampleAppointment])
    expect(wrapper.emitted('delete')?.[0]).toEqual([sampleAppointment])
  })

  it('does not render old standalone phone, position, and address columns', async () => {
    const wrapper = await mountTable()
    const columns = wrapper.findAllComponents(ElTableColumn)
    const columnLabels = columns.map((column) => column.props('label'))
    const columnProps = columns.map((column) => column.props('prop'))

    expect(columnLabels).not.toContain('电话')
    expect(columnLabels).not.toContain('职位')
    expect(columnLabels).not.toContain('地址')
    expect(columnProps).not.toContain('positionName')
    expect(columnProps).not.toContain('graduationSchool')
    expect(columnProps).not.toContain('address')
  })
})
