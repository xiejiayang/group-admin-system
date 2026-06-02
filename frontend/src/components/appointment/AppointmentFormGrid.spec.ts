import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'

import { PARTY_HR_BOARD_DEPARTMENTS } from '@/types/appointment'

import AppointmentFormGrid from './AppointmentFormGrid.vue'

describe('AppointmentFormGrid', () => {
  it('renders the appointment approval form grid with board fields and editable textareas', () => {
    const wrapper = mount(AppointmentFormGrid, {
      props: {
        modelValue: {
          companyName: '集团公司',
          phone: '13800000000',
          idCard: '110101199001011234',
          politicalStatus: '中共党员',
          maritalStatus: '已婚',
          remark: '干部储备',
          positionName: '旧职位',
          graduationSchool: '旧毕业院校',
          address: '旧地址'
        },
        mode: 'edit'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const text = wrapper.text()

    expect(text).toContain('任免审批表')
    expect(text).toContain('所属公司')
    expect(text).toContain('所属部门')
    expect(text).toContain('联系方式（手机长号）')
    expect(text).toContain('身份证号')
    expect(text).toContain('政治面貌')
    expect(text).toContain('婚姻状况')
    expect(text).toContain('备注')
    expect(text).toContain('熟悉专业有何专长')
    expect(text).toContain('审批机关意见')
    expect(text).toContain('行政机关任免意见')
    expect(text).toContain('填表人')
    expect(text).not.toContain('电话')
    expect(text).not.toContain('职位')
    expect(text).not.toContain('地址')
    expect(text).not.toContain('毕业院校系及专业')
    expect(text).not.toContain('（盖章）')
    expect(text).not.toContain('计算机')
    expect(text).not.toContain('（岁）')

    const inputValues = wrapper.findAll('input').map((input) => input.element.value)
    const textareaValues = wrapper.findAll('textarea').map((textarea) => textarea.element.value)

    expect(inputValues).toContain('集团公司')
    expect(inputValues).toContain('13800000000')
    expect(inputValues).toContain('110101199001011234')
    expect(inputValues).toContain('中共党员')
    expect(inputValues).toContain('已婚')
    expect(inputValues).not.toContain('旧职位')
    expect(inputValues).not.toContain('旧毕业院校')
    expect(inputValues).not.toContain('旧地址')
    expect(text).not.toContain('无')
    expect(textareaValues).toContain('干部储备')
    expect(textareaValues).toContain('此表信息已认定')
    expect(textareaValues.length).toBeGreaterThan(0)
  })

  it('renders department radio options from the party HR board configuration', () => {
    const wrapper = mount(AppointmentFormGrid, {
      props: {
        modelValue: {
          departmentName: '融资管理部'
        },
        mode: 'edit'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const text = wrapper.text()
    const departmentRadios = wrapper.findAll('.department-radio-group .el-radio')

    PARTY_HR_BOARD_DEPARTMENTS.forEach((department) => {
      expect(text).toContain(department)
    })
    expect(departmentRadios).toHaveLength(PARTY_HR_BOARD_DEPARTMENTS.length)
    expect(wrapper.find('input[type="radio"][value="融资管理部"]').element.checked).toBe(true)
  })

  it('renders split education fields without the old combined school-major field', () => {
    const wrapper = mount(AppointmentFormGrid, {
      props: {
        modelValue: {
          fullTimeEducation: '本科',
          fullTimeEducationDegree: '学士',
          fullTimeSchool: '清华大学',
          fullTimeMajor: '法学',
          fullTimeSchoolMajor: '旧全日制混合字段',
          partTimeEducation: '研究生',
          partTimeDegree: '硕士',
          partTimeSchool: '北京大学',
          partTimeMajor: '工商管理',
          inServiceEducation: '旧在职教育',
          inServiceSchoolMajor: '旧在职混合字段'
        },
        mode: 'edit'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const text = wrapper.text()
    const inputValues = wrapper.findAll('input').map((input) => input.element.value)

    expect(text).toContain('学历（全日制）')
    expect(text).toContain('学位（全日制）')
    expect(text).toContain('毕业院校（全日制）')
    expect(text).toContain('专业（全日制）')
    expect(text).toContain('学历（非全日制）')
    expect(text).toContain('学位（非全日制）')
    expect(text).toContain('毕业院校（非全日制）')
    expect(text).toContain('专业（非全日制）')
    expect(text).not.toContain('毕业院校系及专业')

    expect(inputValues).toContain('本科')
    expect(inputValues).toContain('学士')
    expect(inputValues).toContain('清华大学')
    expect(inputValues).toContain('法学')
    expect(inputValues).toContain('研究生')
    expect(inputValues).toContain('硕士')
    expect(inputValues).toContain('北京大学')
    expect(inputValues).toContain('工商管理')
    expect(inputValues).not.toContain('旧全日制混合字段')
    expect(inputValues).not.toContain('旧在职教育')
    expect(inputValues).not.toContain('旧在职混合字段')
  })

  it.each([
    ['view mode', { mode: 'view' as const }],
    ['readonly prop', { mode: 'edit' as const, readonly: true }]
  ])('disables form fields and hides photo upload controls in %s', (_label, readonlyProps) => {
    const wrapper = mount(AppointmentFormGrid, {
      props: {
        modelValue: {
          name: '张三',
          approvalAuthorityOpinion: '此表信息已认定'
        },
        ...readonlyProps
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const disabledInputs = wrapper.findAll('input').filter((input) => input.attributes('disabled') !== undefined)
    const disabledTextareas = wrapper
      .findAll('textarea')
      .filter((textarea) => textarea.attributes('disabled') !== undefined)

    expect(disabledInputs.length).toBeGreaterThan(0)
    expect(disabledTextareas.length).toBeGreaterThan(0)
    expect(wrapper.find('input[type="file"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('导入照片')
  })
})
