export const DEFAULT_APPROVAL_AUTHORITY_OPINION = '此表信息已认定'
export const DEFAULT_COMPANY_NAME = '集团公司'
export const DEFAULT_BOARD_DEPARTMENT = '党群人力部'

export const PARTY_HR_BOARD_DEPARTMENTS = [
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
] as const

export type AppointmentFormMode = 'view' | 'create' | 'edit'

export interface AppointmentFamilyMember {
  id?: number
  relationship: string
  name: string
  age: number | null
  politicalStatus: string
  workUnitAndPosition: string
  sortOrder: number
}

export interface AppointmentDetailFamilyMember {
  id?: number | null
  relationship: string | null
  name: string | null
  age: number | null
  politicalStatus: string | null
  workUnitAndPosition: string | null
  sortOrder: number | null
}

export interface AppointmentSummary {
  id: number
  globalSequence: number
  displaySequence: number
  companyName: string
  departmentName: string
  name: string
  currentPosition: string | null
  gender: string | null
  ethnicity: string | null
  idCard: string
  age: number | null
  politicalStatus: string | null
  fullTimeEducation: string | null
  fullTimeEducationDegree: string | null
  fullTimeSchool: string | null
  fullTimeMajor: string | null
  partTimeEducation: string | null
  partTimeDegree: string | null
  partTimeSchool: string | null
  partTimeMajor: string | null
  technicalPosition: string | null
  phone: string
  maritalStatus: string | null
  remark: string | null
}

export interface AppointmentPage {
  items: AppointmentSummary[]
  total: number
  page: number
  size: number
}

export interface AppointmentFormPayload {
  companyName: string
  departmentName: string
  name: string
  phone: string
  idCard: string
  positionName: string
  graduationSchool: string
  address: string
  gender: string
  birthDate: string
  ethnicity: string
  nativePlace: string
  birthPlace: string
  partyJoinDate: string
  workStartDate: string
  healthStatus: string
  politicalStatus: string
  technicalPosition: string
  specialty: string
  fullTimeEducation: string
  fullTimeEducationDegree: string
  fullTimeSchool: string
  fullTimeMajor: string
  fullTimeSchoolMajor: string
  partTimeEducation: string
  partTimeDegree: string
  partTimeSchool: string
  partTimeMajor: string
  inServiceEducation: string
  inServiceSchoolMajor: string
  maritalStatus: string
  currentPosition: string
  proposedPosition: string
  proposedRemovalPosition: string
  resumeText: string
  rewardPunishment: string
  annualAssessmentResult: string
  appointmentReason: string
  reportingUnit: string
  reportingUnitDate: string
  approvalAuthorityOpinion: string
  approvalAuthorityDate: string
  administrativeAppointmentOpinion: string
  administrativeAppointmentDate: string
  formFiller: string
  remark: string
  photoFileId: number | null
  familyMembers: AppointmentFamilyMember[]
}

export type AppointmentDetail = {
  id: number
  globalSequence: number
  displaySequence: number
  age: number | null
  familyMembers: AppointmentDetailFamilyMember[]
} & {
  [K in keyof Omit<AppointmentFormPayload, 'familyMembers'>]: AppointmentFormPayload[K] extends string
    ? string | null
    : AppointmentFormPayload[K]
}

export type AppointmentFamilyMemberModel = Partial<{
  id: number | null
  relationship: string | null
  name: string | null
  age: number | null
  politicalStatus: string | null
  workUnitAndPosition: string | null
  sortOrder: number | null
}>

export type AppointmentFormModel = Partial<{
  [K in keyof AppointmentFormPayload]: K extends 'familyMembers'
    ? AppointmentFamilyMemberModel[] | null
    : AppointmentFormPayload[K] extends string
      ? string | null
      : AppointmentFormPayload[K]
}> & {
  id?: number
  globalSequence?: number
  displaySequence?: number
  age?: number | null
}

export const createEmptyFamilyMember = (sortOrder = 1): AppointmentFamilyMember => ({
  relationship: '',
  name: '',
  age: null,
  politicalStatus: '',
  workUnitAndPosition: '',
  sortOrder
})

export const createEmptyAppointmentForm = (): AppointmentFormPayload => ({
  companyName: DEFAULT_COMPANY_NAME,
  departmentName: DEFAULT_BOARD_DEPARTMENT,
  name: '',
  phone: '',
  idCard: '',
  positionName: '',
  graduationSchool: '',
  address: '',
  gender: '',
  birthDate: '',
  ethnicity: '',
  nativePlace: '',
  birthPlace: '',
  partyJoinDate: '',
  workStartDate: '',
  healthStatus: '',
  politicalStatus: '',
  technicalPosition: '',
  specialty: '',
  fullTimeEducation: '',
  fullTimeEducationDegree: '',
  fullTimeSchool: '',
  fullTimeMajor: '',
  fullTimeSchoolMajor: '',
  partTimeEducation: '',
  partTimeDegree: '',
  partTimeSchool: '',
  partTimeMajor: '',
  inServiceEducation: '',
  inServiceSchoolMajor: '',
  maritalStatus: '',
  currentPosition: '',
  proposedPosition: '',
  proposedRemovalPosition: '',
  resumeText: '',
  rewardPunishment: '',
  annualAssessmentResult: '',
  appointmentReason: '',
  reportingUnit: '',
  reportingUnitDate: '',
  approvalAuthorityOpinion: DEFAULT_APPROVAL_AUTHORITY_OPINION,
  approvalAuthorityDate: '',
  administrativeAppointmentOpinion: '',
  administrativeAppointmentDate: '',
  formFiller: '',
  remark: '',
  photoFileId: null,
  familyMembers: [createEmptyFamilyMember()]
})

const normalizeFamilyMember = (
  member: AppointmentFamilyMemberModel,
  index: number
): AppointmentFamilyMember => ({
  id: member.id ?? undefined,
  relationship: member.relationship ?? '',
  name: member.name ?? '',
  age: member.age ?? null,
  politicalStatus: member.politicalStatus ?? '',
  workUnitAndPosition: member.workUnitAndPosition ?? '',
  sortOrder: member.sortOrder ?? index + 1
})

export const normalizeAppointmentForm = (value: AppointmentFormModel | null | undefined): AppointmentFormPayload => {
  const familyMembers = value?.familyMembers?.map(normalizeFamilyMember) ?? [createEmptyFamilyMember()]

  return {
    companyName: value?.companyName ?? DEFAULT_COMPANY_NAME,
    departmentName: value?.departmentName ?? DEFAULT_BOARD_DEPARTMENT,
    name: value?.name ?? '',
    phone: value?.phone ?? '',
    idCard: value?.idCard ?? '',
    positionName: value?.positionName ?? '',
    graduationSchool: value?.graduationSchool ?? '',
    address: value?.address ?? '',
    gender: value?.gender ?? '',
    birthDate: value?.birthDate ?? '',
    ethnicity: value?.ethnicity ?? '',
    nativePlace: value?.nativePlace ?? '',
    birthPlace: value?.birthPlace ?? '',
    partyJoinDate: value?.partyJoinDate ?? '',
    workStartDate: value?.workStartDate ?? '',
    healthStatus: value?.healthStatus ?? '',
    politicalStatus: value?.politicalStatus ?? '',
    technicalPosition: value?.technicalPosition ?? '',
    specialty: value?.specialty ?? '',
    fullTimeEducation: value?.fullTimeEducation ?? '',
    fullTimeEducationDegree: value?.fullTimeEducationDegree ?? '',
    fullTimeSchool: value?.fullTimeSchool ?? '',
    fullTimeMajor: value?.fullTimeMajor ?? '',
    fullTimeSchoolMajor: value?.fullTimeSchoolMajor ?? '',
    partTimeEducation: value?.partTimeEducation ?? '',
    partTimeDegree: value?.partTimeDegree ?? '',
    partTimeSchool: value?.partTimeSchool ?? '',
    partTimeMajor: value?.partTimeMajor ?? '',
    inServiceEducation: value?.inServiceEducation ?? '',
    inServiceSchoolMajor: value?.inServiceSchoolMajor ?? '',
    maritalStatus: value?.maritalStatus ?? '',
    currentPosition: value?.currentPosition ?? '',
    proposedPosition: value?.proposedPosition ?? '',
    proposedRemovalPosition: value?.proposedRemovalPosition ?? '',
    resumeText: value?.resumeText ?? '',
    rewardPunishment: value?.rewardPunishment ?? '',
    annualAssessmentResult: value?.annualAssessmentResult ?? '',
    appointmentReason: value?.appointmentReason ?? '',
    reportingUnit: value?.reportingUnit ?? '',
    reportingUnitDate: value?.reportingUnitDate ?? '',
    remark: value?.remark ?? '',
    approvalAuthorityOpinion: value?.approvalAuthorityOpinion ?? DEFAULT_APPROVAL_AUTHORITY_OPINION,
    approvalAuthorityDate: value?.approvalAuthorityDate ?? '',
    administrativeAppointmentOpinion: value?.administrativeAppointmentOpinion ?? '',
    administrativeAppointmentDate: value?.administrativeAppointmentDate ?? '',
    formFiller: value?.formFiller ?? '',
    photoFileId: value?.photoFileId ?? null,
    familyMembers: familyMembers.length > 0 ? familyMembers : [createEmptyFamilyMember()]
  }
}

export const sanitizeAppointmentPayload = (value: AppointmentFormModel): AppointmentFormPayload => {
  const normalized = normalizeAppointmentForm(value)
  const familyMembers = normalized.familyMembers
    .filter((member) => {
      return (
        member.relationship.trim() ||
        member.name.trim() ||
        member.age !== null ||
        member.politicalStatus.trim() ||
        member.workUnitAndPosition.trim()
      )
    })
    .map((member, index) => ({
      ...member,
      sortOrder: index + 1
    }))

  return {
    companyName: normalized.companyName,
    departmentName: normalized.departmentName,
    name: normalized.name,
    phone: normalized.phone,
    idCard: normalized.idCard,
    positionName: normalized.positionName,
    graduationSchool: normalized.graduationSchool,
    address: normalized.address,
    gender: normalized.gender,
    birthDate: normalized.birthDate,
    ethnicity: normalized.ethnicity,
    nativePlace: normalized.nativePlace,
    birthPlace: normalized.birthPlace,
    partyJoinDate: normalized.partyJoinDate,
    workStartDate: normalized.workStartDate,
    healthStatus: normalized.healthStatus,
    politicalStatus: normalized.politicalStatus,
    technicalPosition: normalized.technicalPosition,
    specialty: normalized.specialty,
    fullTimeEducation: normalized.fullTimeEducation,
    fullTimeEducationDegree: normalized.fullTimeEducationDegree,
    fullTimeSchool: normalized.fullTimeSchool,
    fullTimeMajor: normalized.fullTimeMajor,
    fullTimeSchoolMajor: normalized.fullTimeSchoolMajor,
    partTimeEducation: normalized.partTimeEducation,
    partTimeDegree: normalized.partTimeDegree,
    partTimeSchool: normalized.partTimeSchool,
    partTimeMajor: normalized.partTimeMajor,
    inServiceEducation: normalized.inServiceEducation,
    inServiceSchoolMajor: normalized.inServiceSchoolMajor,
    maritalStatus: normalized.maritalStatus,
    currentPosition: normalized.currentPosition,
    proposedPosition: normalized.proposedPosition,
    proposedRemovalPosition: normalized.proposedRemovalPosition,
    resumeText: normalized.resumeText,
    rewardPunishment: normalized.rewardPunishment,
    annualAssessmentResult: normalized.annualAssessmentResult,
    appointmentReason: normalized.appointmentReason,
    reportingUnit: normalized.reportingUnit,
    reportingUnitDate: normalized.reportingUnitDate,
    approvalAuthorityOpinion: normalized.approvalAuthorityOpinion,
    approvalAuthorityDate: normalized.approvalAuthorityDate,
    administrativeAppointmentOpinion: normalized.administrativeAppointmentOpinion,
    administrativeAppointmentDate: normalized.administrativeAppointmentDate,
    formFiller: normalized.formFiller,
    remark: normalized.remark,
    photoFileId: normalized.photoFileId,
    familyMembers
  }
}
