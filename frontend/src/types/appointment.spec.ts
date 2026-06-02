import { describe, expect, it } from 'vitest'

import {
  DEFAULT_BOARD_DEPARTMENT,
  DEFAULT_COMPANY_NAME,
  PARTY_HR_BOARD_DEPARTMENTS,
  createEmptyAppointmentForm,
  normalizeAppointmentForm,
  sanitizeAppointmentPayload
} from './appointment'

describe('appointment types', () => {
  it('creates an empty appointment form with board defaults', () => {
    const form = createEmptyAppointmentForm()

    expect(DEFAULT_COMPANY_NAME).toBe('集团公司')
    expect(DEFAULT_BOARD_DEPARTMENT).toBe('党群人力部')
    expect(form.companyName).toBe('集团公司')
    expect(form.departmentName).toBe('党群人力部')
  })

  it('exposes party HR board department options', () => {
    expect(PARTY_HR_BOARD_DEPARTMENTS).toEqual([
      '领导班子',
      '专家顾问',
      '董事会办公室',
      '财务管理部',
      '纪检监察部',
      '党群人力部',
      '综合管理部',
      '融资管理部',
      '产业发展部',
      '法务风控部',
      '建设管理部'
    ])
  })

  it('normalizes new board fields without dropping provided values', () => {
    const form = normalizeAppointmentForm({
      companyName: '二级公司',
      departmentName: '法务风控部',
      politicalStatus: '中共党员',
      fullTimeEducationDegree: '学士',
      fullTimeSchool: '清华大学',
      fullTimeMajor: '法学',
      partTimeEducation: '研究生',
      partTimeDegree: '硕士',
      partTimeSchool: '北京大学',
      partTimeMajor: '工商管理',
      maritalStatus: '已婚',
      remark: '干部储备'
    })

    expect(form).toMatchObject({
      companyName: '二级公司',
      departmentName: '法务风控部',
      politicalStatus: '中共党员',
      fullTimeEducationDegree: '学士',
      fullTimeSchool: '清华大学',
      fullTimeMajor: '法学',
      partTimeEducation: '研究生',
      partTimeDegree: '硕士',
      partTimeSchool: '北京大学',
      partTimeMajor: '工商管理',
      maritalStatus: '已婚',
      remark: '干部储备'
    })
  })

  it('normalizes explicit undefined board fields back to defaults', () => {
    const form = normalizeAppointmentForm({
      companyName: undefined,
      departmentName: undefined,
      partTimeMajor: undefined,
      remark: undefined
    })

    expect(form.companyName).toBe('集团公司')
    expect(form.departmentName).toBe('党群人力部')
    expect(form.partTimeMajor).toBe('')
    expect(form.remark).toBe('')
  })

  it('normalizes nullable detail-like input without readonly response fields', () => {
    const form = normalizeAppointmentForm({
      id: 42,
      globalSequence: 7,
      displaySequence: 3,
      age: 45,
      companyName: null,
      departmentName: null,
      name: null,
      phone: null,
      idCard: null,
      gender: null,
      birthDate: null,
      ethnicity: null,
      politicalStatus: null,
      fullTimeEducation: null,
      fullTimeEducationDegree: null,
      fullTimeSchool: null,
      fullTimeMajor: null,
      partTimeEducation: null,
      partTimeDegree: null,
      partTimeSchool: null,
      partTimeMajor: null,
      technicalPosition: null,
      maritalStatus: null,
      currentPosition: null,
      reportingUnitDate: null,
      approvalAuthorityOpinion: null,
      remark: null,
      familyMembers: [
        {
          id: null,
          relationship: null,
          name: null,
          age: null,
          politicalStatus: null,
          workUnitAndPosition: null,
          sortOrder: null
        }
      ]
    })

    expect('id' in form).toBe(false)
    expect('globalSequence' in form).toBe(false)
    expect('displaySequence' in form).toBe(false)
    expect('age' in form).toBe(false)
    expect(form).toMatchObject({
      companyName: '集团公司',
      departmentName: '党群人力部',
      name: '',
      phone: '',
      idCard: '',
      gender: '',
      birthDate: '',
      ethnicity: '',
      politicalStatus: '',
      fullTimeEducation: '',
      fullTimeEducationDegree: '',
      fullTimeSchool: '',
      fullTimeMajor: '',
      partTimeEducation: '',
      partTimeDegree: '',
      partTimeSchool: '',
      partTimeMajor: '',
      technicalPosition: '',
      maritalStatus: '',
      currentPosition: '',
      reportingUnitDate: '',
      approvalAuthorityOpinion: '此表信息已认定',
      remark: ''
    })
    expect(form.familyMembers).toEqual([
      {
        id: undefined,
        relationship: '',
        name: '',
        age: null,
        politicalStatus: '',
        workUnitAndPosition: '',
        sortOrder: 1
      }
    ])
  })

  it('sanitizes appointment payload while preserving board and existing API fields', () => {
    const payload = sanitizeAppointmentPayload({
      companyName: '集团公司',
      departmentName: '融资管理部',
      name: '张三',
      currentPosition: '招商主管',
      phone: '13800000000',
      partTimeMajor: '金融学',
      remark: '拟调整'
    })

    expect(payload).toMatchObject({
      companyName: '集团公司',
      departmentName: '融资管理部',
      name: '张三',
      currentPosition: '招商主管',
      phone: '13800000000',
      partTimeMajor: '金融学',
      remark: '拟调整'
    })
  })

  it('sanitizes detail-like input without readonly response fields', () => {
    const payload = sanitizeAppointmentPayload({
      id: 42,
      globalSequence: 7,
      displaySequence: 3,
      age: 45,
      companyName: '集团公司',
      departmentName: '党群人力部',
      name: '李四',
      currentPosition: '党群主管',
      phone: '13900001111'
    })

    expect('id' in payload).toBe(false)
    expect('globalSequence' in payload).toBe(false)
    expect('displaySequence' in payload).toBe(false)
    expect('age' in payload).toBe(false)
  })

  it('sanitizes family members by preserving non-empty rows and dropping blank placeholders', () => {
    const payload = sanitizeAppointmentPayload({
      familyMembers: [
        {
          relationship: '配偶',
          name: '王五',
          age: 40,
          politicalStatus: '',
          workUnitAndPosition: '集团公司',
          sortOrder: 8
        },
        {
          relationship: null,
          name: '',
          age: null,
          politicalStatus: null,
          workUnitAndPosition: null,
          sortOrder: null
        }
      ]
    })

    expect(payload.familyMembers).toEqual([
      {
        relationship: '配偶',
        name: '王五',
        age: 40,
        politicalStatus: '',
        workUnitAndPosition: '集团公司',
        sortOrder: 1
      }
    ])
  })
})
