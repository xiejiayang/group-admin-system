<script setup lang="ts">
import { UploadFilled } from '@element-plus/icons-vue'
import { ElButton, ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

import { fileReadUrl, uploadIdPhoto, type FileUploadResponse } from '@/api/file'

const props = defineProps<{
  modelValue?: number | null
  readonly?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
  uploaded: [file: FileUploadResponse]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const previewUrl = computed(() => {
  return props.modelValue ? fileReadUrl(props.modelValue) : ''
})

const openFilePicker = () => {
  fileInput.value?.click()
}

const validateFile = (file: File) => {
  const allowedTypes = ['image/jpeg', 'image/png']

  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('请上传 JPG 或 PNG 格式的一寸证件照')
    return false
  }

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('证件照大小不能超过 2MB')
    return false
  }

  return true
}

const toMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message : fallback
}

const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file || props.readonly) {
    input.value = ''
    return
  }

  if (!validateFile(file)) {
    input.value = ''
    return
  }

  uploading.value = true

  try {
    // 照片上传成功后只把文件编号回填到任免表，主表保存时再引用该编号。
    const uploadedFile = await uploadIdPhoto(file)
    emit('update:modelValue', uploadedFile.id)
    emit('uploaded', uploadedFile)
    ElMessage.success('证件照已上传')
  } catch (error) {
    ElMessage.error(toMessage(error, '证件照上传失败'))
  } finally {
    uploading.value = false
    input.value = ''
  }
}
</script>

<template>
  <div class="photo-uploader" :class="{ 'is-readonly': readonly }">
    <div class="photo-frame">
      <img v-if="previewUrl" :src="previewUrl" alt="证件照" />
      <span v-else>照片</span>
    </div>

    <template v-if="!readonly">
      <input
        ref="fileInput"
        class="photo-file-input"
        type="file"
        accept="image/jpeg,image/png"
        @change="handleFileChange"
      />
      <el-button :icon="UploadFilled" :loading="uploading" size="small" @click="openFilePicker">导入照片</el-button>
    </template>
  </div>
</template>

<style scoped>
.photo-uploader {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.photo-frame {
  width: 96px;
  aspect-ratio: 25 / 35;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #9ca3af;
  background: #f8fafc;
  color: #64748b;
  font-size: 14px;
  line-height: 1.4;
}

.photo-frame img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.photo-file-input {
  display: none;
}

.photo-uploader.is-readonly .photo-frame {
  background: #ffffff;
}
</style>
