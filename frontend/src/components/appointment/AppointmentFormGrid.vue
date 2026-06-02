<script setup lang="ts">
import { CirclePlus, Delete } from '@element-plus/icons-vue'
import {
  ElButton,
  ElDatePicker,
  ElInput,
  ElInputNumber,
  ElRadio,
  ElRadioGroup,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { computed, nextTick, ref, watch } from 'vue'

import PhotoUploader from './PhotoUploader.vue'

import {
  PARTY_HR_BOARD_DEPARTMENTS,
  createEmptyFamilyMember,
  normalizeAppointmentForm,
  type AppointmentFormMode,
  type AppointmentFormModel,
  type AppointmentFormPayload
} from '@/types/appointment'

const props = withDefaults(
  defineProps<{
    modelValue: AppointmentFormModel
    mode?: AppointmentFormMode
    readonly?: boolean
  }>(),
  {
    mode: 'edit',
    readonly: undefined
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: AppointmentFormPayload]
}>()

// 表单字段按后端 AppointmentRecordRequest 命名，界面标签保持任免审批表和看板列的中文语义。
const form = ref<AppointmentFormPayload>(normalizeAppointmentForm(props.modelValue))
const applyingExternalValue = ref(false)

// view 模式通过同一套网格禁用输入，create/edit 模式保持双向回填供弹窗保存。
const isReadonly = computed(() => props.readonly ?? props.mode === 'view')

// 部门选项来源于党群人力看板配置，保存时与审批表 departmentName 字段保持一致。
const departmentOptions = PARTY_HR_BOARD_DEPARTMENTS

watch(
  () => props.modelValue,
  async (value) => {
    applyingExternalValue.value = true
    form.value = normalizeAppointmentForm(value)
    await nextTick()
    applyingExternalValue.value = false
  },
  { deep: true, immediate: true }
)

watch(
  form,
  (value) => {
    if (!applyingExternalValue.value) {
      emit('update:modelValue', normalizeAppointmentForm(value))
    }
  },
  { deep: true }
)

const addFamilyMember = () => {
  form.value.familyMembers.push(createEmptyFamilyMember(form.value.familyMembers.length + 1))
}

const removeFamilyMember = (index: number) => {
  if (form.value.familyMembers.length === 1) {
    form.value.familyMembers = [createEmptyFamilyMember()]
    return
  }

  form.value.familyMembers.splice(index, 1)
  form.value.familyMembers.forEach((member, memberIndex) => {
    member.sortOrder = memberIndex + 1
  })
}
</script>

<template>
  <section class="appointment-form-grid" :class="{ 'is-readonly': isReadonly }">
    <h2>任免审批表</h2>

    <div class="board-field-grid">
      <label class="board-form-field">
        <span>所属公司</span>
        <el-input v-model="form.companyName" :disabled="isReadonly" />
      </label>
      <label class="board-form-field department-field">
        <span>所属部门</span>
        <el-radio-group v-model="form.departmentName" class="department-radio-group" :disabled="isReadonly">
          <el-radio v-for="department in departmentOptions" :key="department" :value="department">
            {{ department }}
          </el-radio>
        </el-radio-group>
      </label>
      <label class="board-form-field">
        <span>联系方式（手机长号）</span>
        <el-input v-model="form.phone" :disabled="isReadonly" />
      </label>
      <label class="board-form-field">
        <span>身份证号</span>
        <el-input v-model="form.idCard" :disabled="isReadonly" />
      </label>
      <label class="board-form-field">
        <span>政治面貌</span>
        <el-input v-model="form.politicalStatus" :disabled="isReadonly" />
      </label>
      <label class="board-form-field">
        <span>婚姻状况</span>
        <el-input v-model="form.maritalStatus" :disabled="isReadonly" />
      </label>
      <label class="board-form-field full">
        <span>备注</span>
        <el-input v-model="form.remark" :disabled="isReadonly" :rows="3" type="textarea" />
      </label>
    </div>

    <div class="approval-table-shell">
      <table class="approval-table">
        <colgroup>
          <col class="label-col" />
          <col class="value-col" />
          <col class="label-col" />
          <col class="value-col" />
          <col class="label-col" />
          <col class="value-col" />
          <col class="label-col" />
          <col class="value-col" />
        </colgroup>
        <tbody>
          <tr>
            <th>姓名</th>
            <td><el-input v-model="form.name" :disabled="isReadonly" /></td>
            <th>性别</th>
            <td><el-input v-model="form.gender" :disabled="isReadonly" /></td>
            <th>出生年月</th>
            <td>
              <el-date-picker
                v-model="form.birthDate"
                :disabled="isReadonly"
                format="YYYY-MM-DD"
                placeholder="选择日期"
                type="date"
                value-format="YYYY-MM-DD"
              />
            </td>
            <td class="photo-cell" colspan="2" rowspan="4">
              <photo-uploader v-model="form.photoFileId" :readonly="isReadonly" />
            </td>
          </tr>

          <tr>
            <th>民族</th>
            <td><el-input v-model="form.ethnicity" :disabled="isReadonly" /></td>
            <th>籍贯</th>
            <td><el-input v-model="form.nativePlace" :disabled="isReadonly" /></td>
            <th>出生地</th>
            <td><el-input v-model="form.birthPlace" :disabled="isReadonly" /></td>
          </tr>

          <tr>
            <th>入党时间</th>
            <td>
              <el-date-picker
                v-model="form.partyJoinDate"
                :disabled="isReadonly"
                format="YYYY-MM-DD"
                placeholder="选择日期"
                type="date"
                value-format="YYYY-MM-DD"
              />
            </td>
            <th>参加工作时间</th>
            <td>
              <el-date-picker
                v-model="form.workStartDate"
                :disabled="isReadonly"
                format="YYYY-MM-DD"
                placeholder="选择日期"
                type="date"
                value-format="YYYY-MM-DD"
              />
            </td>
            <th>健康状况</th>
            <td><el-input v-model="form.healthStatus" :disabled="isReadonly" /></td>
          </tr>

          <tr>
            <th>专业技术职务</th>
            <td><el-input v-model="form.technicalPosition" :disabled="isReadonly" /></td>
            <th>熟悉专业有何专长</th>
            <td colspan="3"><el-input v-model="form.specialty" :disabled="isReadonly" /></td>
          </tr>

          <tr>
            <th>学历（全日制）</th>
            <td><el-input v-model="form.fullTimeEducation" :disabled="isReadonly" /></td>
            <th>学位（全日制）</th>
            <td><el-input v-model="form.fullTimeEducationDegree" :disabled="isReadonly" /></td>
            <th>毕业院校（全日制）</th>
            <td><el-input v-model="form.fullTimeSchool" :disabled="isReadonly" /></td>
            <th>专业（全日制）</th>
            <td><el-input v-model="form.fullTimeMajor" :disabled="isReadonly" /></td>
          </tr>
          <tr>
            <th>学历（非全日制）</th>
            <td><el-input v-model="form.partTimeEducation" :disabled="isReadonly" /></td>
            <th>学位（非全日制）</th>
            <td><el-input v-model="form.partTimeDegree" :disabled="isReadonly" /></td>
            <th>毕业院校（非全日制）</th>
            <td><el-input v-model="form.partTimeSchool" :disabled="isReadonly" /></td>
            <th>专业（非全日制）</th>
            <td><el-input v-model="form.partTimeMajor" :disabled="isReadonly" /></td>
          </tr>

          <tr>
            <th>现任职务</th>
            <td colspan="7"><el-input v-model="form.currentPosition" :disabled="isReadonly" /></td>
          </tr>
          <tr>
            <th>拟任职务</th>
            <td colspan="7"><el-input v-model="form.proposedPosition" :disabled="isReadonly" /></td>
          </tr>
          <tr>
            <th>拟免职务</th>
            <td colspan="7"><el-input v-model="form.proposedRemovalPosition" :disabled="isReadonly" /></td>
          </tr>

          <tr>
            <th>简历</th>
            <td colspan="7">
              <el-input v-model="form.resumeText" :disabled="isReadonly" :rows="5" type="textarea" />
            </td>
          </tr>
          <tr>
            <th>奖惩情况</th>
            <td colspan="7">
              <el-input v-model="form.rewardPunishment" :disabled="isReadonly" :rows="3" type="textarea" />
            </td>
          </tr>
          <tr>
            <th>年度考核结果</th>
            <td colspan="7">
              <el-input v-model="form.annualAssessmentResult" :disabled="isReadonly" :rows="3" type="textarea" />
            </td>
          </tr>
          <tr>
            <th>任免理由</th>
            <td colspan="7">
              <el-input v-model="form.appointmentReason" :disabled="isReadonly" :rows="4" type="textarea" />
            </td>
          </tr>

          <tr>
            <th>家庭主要成员及重要社会关系</th>
            <td colspan="7">
              <div class="family-editor">
                <el-table :data="form.familyMembers" border class="family-table" size="small">
                  <el-table-column label="称谓" min-width="110">
                    <template #default="{ row }">
                      <el-input v-model="row.relationship" :disabled="isReadonly" />
                    </template>
                  </el-table-column>
                  <el-table-column label="姓名" min-width="110">
                    <template #default="{ row }">
                      <el-input v-model="row.name" :disabled="isReadonly" />
                    </template>
                  </el-table-column>
                  <el-table-column label="年龄" width="96">
                    <template #default="{ row }">
                      <el-input-number
                        v-model="row.age"
                        :controls="false"
                        :disabled="isReadonly"
                        :max="150"
                        :min="0"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="政治面貌" min-width="120">
                    <template #default="{ row }">
                      <el-input v-model="row.politicalStatus" :disabled="isReadonly" />
                    </template>
                  </el-table-column>
                  <el-table-column label="工作单位及职务" min-width="220">
                    <template #default="{ row }">
                      <el-input v-model="row.workUnitAndPosition" :disabled="isReadonly" />
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!isReadonly" label="操作" width="76">
                    <template #default="{ $index }">
                      <el-button :icon="Delete" link type="danger" @click="removeFamilyMember($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <el-button v-if="!isReadonly" :icon="CirclePlus" plain size="small" @click="addFamilyMember">
                  添加成员
                </el-button>
              </div>
            </td>
          </tr>

          <tr>
            <th>呈报单位</th>
            <td colspan="7">
              <div class="opinion-cell">
                <el-input v-model="form.reportingUnit" :disabled="isReadonly" :rows="4" type="textarea" />
                <el-date-picker
                  v-model="form.reportingUnitDate"
                  :disabled="isReadonly"
                  class="cell-date"
                  format="YYYY-MM-DD"
                  placeholder="选择日期"
                  type="date"
                  value-format="YYYY-MM-DD"
                />
              </div>
            </td>
          </tr>
          <tr>
            <th>审批机关意见</th>
            <td colspan="3">
              <div class="opinion-cell">
                <el-input
                  v-model="form.approvalAuthorityOpinion"
                  :disabled="isReadonly"
                  :rows="4"
                  type="textarea"
                />
                <el-date-picker
                  v-model="form.approvalAuthorityDate"
                  :disabled="isReadonly"
                  class="cell-date"
                  format="YYYY-MM-DD"
                  placeholder="选择日期"
                  type="date"
                  value-format="YYYY-MM-DD"
                />
              </div>
            </td>
            <th>行政机关任免意见</th>
            <td colspan="3">
              <div class="opinion-cell">
                <el-input
                  v-model="form.administrativeAppointmentOpinion"
                  :disabled="isReadonly"
                  :rows="4"
                  type="textarea"
                />
                <el-date-picker
                  v-model="form.administrativeAppointmentDate"
                  :disabled="isReadonly"
                  class="cell-date"
                  format="YYYY-MM-DD"
                  placeholder="选择日期"
                  type="date"
                  value-format="YYYY-MM-DD"
                />
              </div>
            </td>
          </tr>
          <tr>
            <th>填表人</th>
            <td colspan="7"><el-input v-model="form.formFiller" :disabled="isReadonly" /></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.appointment-form-grid {
  color: #111827;
}

.appointment-form-grid h2 {
  margin: 0 0 16px;
  color: #111827;
  font-size: 26px;
  line-height: 1.3;
  text-align: center;
  letter-spacing: 0;
}

.board-field-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid #d1d5db;
  background: #ffffff;
}

.board-field-grid > .board-form-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.board-field-grid .department-field {
  grid-column: span 3;
}

.board-field-grid .full {
  grid-column: 1 / -1;
}

.department-radio-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  gap: 8px;
  width: 100%;
}

.department-radio-group :deep(.el-radio) {
  height: auto;
  min-height: 34px;
  margin-right: 0;
  padding: 6px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #f8fafc;
}

.department-radio-group :deep(.el-radio.is-checked) {
  border-color: #2563eb;
  background: #eff6ff;
}

.board-field-grid .wide {
  grid-column: span 2;
}

.approval-table-shell {
  width: 100%;
  overflow-x: auto;
  border: 1px solid #1f2937;
  background: #ffffff;
}

.approval-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  table-layout: fixed;
  background: #ffffff;
}

.approval-table col.label-col {
  width: 11%;
}

.approval-table col.value-col {
  width: 14%;
}

.approval-table th,
.approval-table td {
  min-height: 48px;
  padding: 8px;
  border: 1px solid #1f2937;
  vertical-align: middle;
}

.approval-table th {
  background: #f8fafc;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  text-align: center;
  overflow-wrap: anywhere;
}

.approval-table td {
  background: #ffffff;
}

.photo-cell {
  height: 196px;
}

.family-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.family-editor > .el-button {
  align-self: flex-start;
}

.family-table {
  width: 100%;
}

.opinion-cell {
  display: flex;
  min-height: 142px;
  flex-direction: column;
  gap: 10px;
}

.cell-date {
  align-self: flex-end;
  width: 168px;
}

.appointment-form-grid :deep(.el-input),
.appointment-form-grid :deep(.el-date-editor.el-input) {
  width: 100%;
}

.appointment-form-grid :deep(.el-input__wrapper),
.appointment-form-grid :deep(.el-textarea__inner) {
  border-radius: 4px;
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.appointment-form-grid :deep(.el-textarea__inner) {
  min-height: 88px;
  resize: vertical;
}

.appointment-form-grid :deep(.el-input-number) {
  width: 100%;
}

.appointment-form-grid :deep(.el-table .cell) {
  padding-right: 6px;
  padding-left: 6px;
}

.appointment-form-grid.is-readonly :deep(.el-input__wrapper),
.appointment-form-grid.is-readonly :deep(.el-textarea__inner) {
  background: #f9fafb;
}

@media (max-width: 720px) {
  .appointment-form-grid h2 {
    font-size: 22px;
  }

  .board-field-grid {
    grid-template-columns: 1fr;
  }

  .board-field-grid .wide {
    grid-column: auto;
  }

  .board-field-grid .department-field,
  .board-field-grid .full {
    grid-column: auto;
  }
}
</style>
