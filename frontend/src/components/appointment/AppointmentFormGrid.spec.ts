import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'

import AppointmentFormGrid from './AppointmentFormGrid.vue'

describe('AppointmentFormGrid', () => {
  it('renders the appointment approval form grid with required labels and editable textareas', () => {
    const wrapper = mount(AppointmentFormGrid, {
      props: {
        modelValue: {},
        mode: 'edit'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const text = wrapper.text()

    expect(text).toContain('任免审批表')
    expect(text).toContain('熟悉专业有何专长')
    expect(text).toContain('审批机关意见')
    expect(text).toContain('行政机关任免意见')
    expect(text).toContain('填表人')
    expect(text).not.toContain('（盖章）')
    expect(text).not.toContain('计算机')
    expect(text).not.toContain('（岁）')

    const textareaValues = wrapper.findAll('textarea').map((textarea) => textarea.element.value)

    expect(text).not.toContain('无')
    expect(textareaValues).toContain('此表信息已认定')
    expect(textareaValues.length).toBeGreaterThan(0)
  })
})
