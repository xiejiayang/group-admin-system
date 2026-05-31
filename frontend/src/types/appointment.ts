export const DEFAULT_APPROVAL_AUTHORITY_OPINION = '此表信息已认定'

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

export interface AppointmentSummary {
  id: number
  name: string
  phone: string
  idCard: string
  positionName: string
  graduationSchool: string
  address: string
}

export interface AppointmentPage {
  items: AppointmentSummary[]
  total: number
  page: number
  size: number
}

export interface AppointmentFormPayload {
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
  technicalPosition: string
  specialty: string
  fullTimeEducation: string
  fullTimeSchoolMajor: string
  inServiceEducation: string
  inServiceSchoolMajor: string
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
  photoFileId: number | null
  familyMembers: AppointmentFamilyMember[]
}

export interface AppointmentDetail extends AppointmentFormPayload {
  id: number
}

export type AppointmentFormModel = Partial<AppointmentFormPayload> & {
  id?: number
  familyMembers?: Partial<AppointmentFamilyMember>[]
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
  technicalPosition: '',
  specialty: '',
  fullTimeEducation: '',
  fullTimeSchoolMajor: '',
  inServiceEducation: '',
  inServiceSchoolMajor: '',
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
  photoFileId: null,
  familyMembers: [createEmptyFamilyMember()]
})

const normalizeFamilyMember = (
  member: Partial<AppointmentFamilyMember>,
  index: number
): AppointmentFamilyMember => ({
  id: member.id,
  relationship: member.relationship ?? '',
  name: member.name ?? '',
  age: member.age ?? null,
  politicalStatus: member.politicalStatus ?? '',
  workUnitAndPosition: member.workUnitAndPosition ?? '',
  sortOrder: member.sortOrder ?? index + 1
})

export const normalizeAppointmentForm = (value: AppointmentFormModel | null | undefined): AppointmentFormPayload => {
  const base = createEmptyAppointmentForm()
  const familyMembers = value?.familyMembers?.map(normalizeFamilyMember) ?? base.familyMembers

  return {
    ...base,
    ...value,
    approvalAuthorityOpinion: value?.approvalAuthorityOpinion ?? DEFAULT_APPROVAL_AUTHORITY_OPINION,
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
    ...normalized,
    familyMembers
  }
}
