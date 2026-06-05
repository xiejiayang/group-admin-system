import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createEmptyAppointmentForm,
  type AppointmentDetail,
  type AppointmentFormPayload
} from '@/types/appointment'

import AppointmentFormDialog from './AppointmentFormDialog.vue'

const createAppointmentMock = vi.hoisted(() => vi.fn())
const fetchAppointmentDetailMock = vi.hoisted(() => vi.fn())
const updateAppointmentMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/appointment', () => ({
  createAppointment: createAppointmentMock,
  fetchAppointmentDetail: fetchAppointmentDetailMock,
  updateAppointment: updateAppointmentMock
}))

const DialogStub = defineComponent({
  name: 'ElDialog',
  props: {
    modelValue: Boolean,
    beforeClose: Function
  },
  emits: ['update:modelValue', 'closed'],
  setup(props, { emit, slots }) {
    const close = () => {
      const done = () => {
        emit('update:modelValue', false)
        emit('closed')
      }

      if (props.beforeClose) {
        props.beforeClose(done)
      } else {
        done()
      }
    }

    return () =>
      h('div', [
        h('button', { class: 'dialog-close', onClick: close }, '关闭弹窗'),
        slots.default?.(),
        slots.footer?.()
      ])
  }
})

const ButtonStub = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  setup(_props, { emit, slots }) {
    return () => h('button', { onClick: () => emit('click') }, slots.default?.())
  }
})

const FormGridStub = defineComponent({
  name: 'AppointmentFormGrid',
  props: {
    modelValue: {
      type: Object,
      required: true
    },
    mode: {
      type: String,
      required: true
    }
  },
  emits: ['update:modelValue'],
  setup() {
    return () => h('div', { class: 'form-grid' })
  }
})

const requiredDraft = (name = '张三'): AppointmentFormPayload => ({
  ...createEmptyAppointmentForm(),
  name,
  phone: '13800000000',
  companyName: '集团公司',
  departmentName: '党群人力部',
  currentPosition: '部门经理'
})

const mountDialog = () =>
  mount(AppointmentFormDialog, {
    props: {
      modelValue: false,
      mode: 'create'
    },
    global: {
      stubs: {
        ElAlert: true,
        ElButton: ButtonStub,
        ElDialog: DialogStub,
        ElSkeleton: true,
        AppointmentFormGrid: FormGridStub
      }
    }
  })

const formGrid = (wrapper: ReturnType<typeof mountDialog>) =>
  wrapper.findComponent({ name: 'AppointmentFormGrid' })

const fillForm = async (wrapper: ReturnType<typeof mountDialog>, payload: AppointmentFormPayload) => {
  formGrid(wrapper).vm.$emit('update:modelValue', payload)
  await nextTick()
}

const clickFooterButton = async (wrapper: ReturnType<typeof mountDialog>, label: string) => {
  const button = wrapper.findAll('button').find((item) => item.text() === label)

  expect(button).toBeDefined()
  await button!.trigger('click')
}

describe('AppointmentFormDialog', () => {
  beforeEach(() => {
    createAppointmentMock.mockReset()
    fetchAppointmentDetailMock.mockReset()
    updateAppointmentMock.mockReset()
    createAppointmentMock.mockResolvedValue({})
  })

  it('reopens a create dialog with the draft retained after cancellation', async () => {
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await fillForm(wrapper, requiredDraft('取消后保留'))

    await clickFooterButton(wrapper, '取消')
    await wrapper.setProps({ modelValue: false })
    await wrapper.setProps({ modelValue: true })

    expect(formGrid(wrapper).props('modelValue')).toMatchObject({ name: '取消后保留' })
  })

  it('reopens a create dialog with the draft retained after the dialog close button', async () => {
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await fillForm(wrapper, requiredDraft('关闭后保留'))

    await wrapper.find('.dialog-close').trigger('click')
    await wrapper.setProps({ modelValue: false })
    await wrapper.setProps({ modelValue: true })

    expect(formGrid(wrapper).props('modelValue')).toMatchObject({ name: '关闭后保留' })
  })

  it('clears the create draft only after a successful save', async () => {
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await fillForm(wrapper, requiredDraft('保存后清空'))

    await clickFooterButton(wrapper, '保存')
    await flushPromises()

    expect(wrapper.emitted('update:modelValue')).toContainEqual([false])

    await wrapper.setProps({ modelValue: false })
    await wrapper.setProps({ modelValue: true })

    expect(createAppointmentMock).toHaveBeenCalledWith(expect.objectContaining({ name: '保存后清空' }))
    expect(wrapper.emitted('refresh')).toHaveLength(1)
    expect(formGrid(wrapper).props('modelValue')).toMatchObject({
      name: '',
      companyName: '集团公司',
      departmentName: '党群人力部'
    })
  })

  it('retains the create draft when saving fails', async () => {
    createAppointmentMock.mockRejectedValueOnce(new Error('保存失败'))
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await fillForm(wrapper, requiredDraft('失败后保留'))

    await clickFooterButton(wrapper, '保存')
    await flushPromises()
    await clickFooterButton(wrapper, '取消')
    await wrapper.setProps({ modelValue: false })
    await wrapper.setProps({ modelValue: true })

    expect(wrapper.emitted('refresh')).toBeUndefined()
    expect(formGrid(wrapper).props('modelValue')).toMatchObject({ name: '失败后保留' })
  })

  it('does not replace the create draft when viewing an existing appointment', async () => {
    fetchAppointmentDetailMock.mockResolvedValueOnce({
      ...requiredDraft('已有记录'),
      id: 8,
      globalSequence: 1,
      displaySequence: 1,
      age: 36
    })
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await fillForm(wrapper, requiredDraft('新增草稿'))
    await clickFooterButton(wrapper, '取消')
    await wrapper.setProps({ modelValue: false, mode: 'view', appointmentId: 8 })
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    expect(formGrid(wrapper).props('modelValue')).toMatchObject({ name: '已有记录' })

    await wrapper.find('.dialog-close').trigger('click')
    await wrapper.setProps({ modelValue: false, mode: 'create', appointmentId: null })
    await wrapper.setProps({ modelValue: true })

    expect(formGrid(wrapper).props('modelValue')).toMatchObject({ name: '新增草稿' })
  })

  it('ignores a stale detail response after returning to the create draft', async () => {
    let resolveDetail!: (detail: AppointmentDetail) => void
    fetchAppointmentDetailMock.mockReturnValueOnce(
      new Promise<AppointmentDetail>((resolve) => {
        resolveDetail = resolve
      })
    )
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await fillForm(wrapper, requiredDraft('竞态新增草稿'))
    await clickFooterButton(wrapper, '取消')

    await wrapper.setProps({ modelValue: false, mode: 'view', appointmentId: 8 })
    await wrapper.setProps({ modelValue: true })
    expect(fetchAppointmentDetailMock).toHaveBeenCalledWith(8)

    await clickFooterButton(wrapper, '关闭')
    await wrapper.setProps({ modelValue: false, mode: 'create', appointmentId: null })
    await wrapper.setProps({ modelValue: true })

    resolveDetail({
      ...requiredDraft('延迟返回的已有记录'),
      id: 8,
      globalSequence: 1,
      displaySequence: 1,
      age: 36
    })
    await flushPromises()

    expect(formGrid(wrapper).props('modelValue')).toMatchObject({ name: '竞态新增草稿' })
  })
})
