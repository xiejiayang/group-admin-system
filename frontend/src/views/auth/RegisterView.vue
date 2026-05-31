<script setup lang="ts">
import { Lock, OfficeBuilding, Phone, Right, User } from '@element-plus/icons-vue'
import {
  ElButton,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMessage,
  ElRadio,
  ElRadioGroup,
  type FormInstance,
  type FormRules
} from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import gateUrl from '@/assets/group-gate.png'
import type { AuthUser, RegisterRequest } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<RegisterRequest>({
  username: '',
  password: '',
  phone: '',
  departmentCode: 'PARTY_HR'
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  departmentCode: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

const shellStyle = {
  backgroundImage: `linear-gradient(90deg, rgba(10, 20, 38, 0.72), rgba(10, 20, 38, 0.28)), url(${gateUrl})`
}

const landingPath = (user: AuthUser) => {
  if (user.departmentCode === 'GENERAL_ADMIN' || user.roles.includes('GENERAL_ADMIN_USER')) {
    return '/general-admin'
  }

  return '/party-hr'
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)

  if (!valid) {
    return
  }

  loading.value = true

  try {
    const authStore = useAuthStore()
    const user = await authStore.register({
      username: form.username.trim(),
      password: form.password,
      phone: form.phone.trim(),
      departmentCode: form.departmentCode
    })

    await router?.push(landingPath(user))
  } catch (error) {
    const message = error instanceof Error ? error.message : '注册失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const goLogin = async () => {
  await router?.push('/login')
}
</script>

<template>
  <main class="auth-shell" :style="shellStyle">
    <section class="auth-panel" aria-label="注册">
      <div class="brand-block">
        <p class="brand-kicker">账号注册</p>
        <h1>集团后台管理系统</h1>
      </div>

      <el-form
        ref="formRef"
        class="auth-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            autocomplete="username"
            :prefix-icon="User"
            placeholder="请输入账号"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            autocomplete="new-password"
            :prefix-icon="Lock"
            placeholder="请输入密码"
            show-password
            size="large"
            type="password"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            autocomplete="tel"
            :prefix-icon="Phone"
            maxlength="11"
            placeholder="请输入手机号"
            size="large"
          />
        </el-form-item>

        <el-form-item label="部门" prop="departmentCode">
          <el-radio-group v-model="form.departmentCode" class="department-group">
            <el-radio value="PARTY_HR">
              <el-icon><OfficeBuilding /></el-icon>
              党群人力部
            </el-radio>
            <el-radio value="GENERAL_ADMIN">
              <el-icon><OfficeBuilding /></el-icon>
              综合管理部
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-button
          class="auth-submit"
          :loading="loading"
          native-type="button"
          size="large"
          type="primary"
          @click="handleRegister"
        >
          <span>注册</span>
          <el-icon><Right /></el-icon>
        </el-button>
      </el-form>

      <div class="auth-extra">
        <span>已有账号？</span>
        <button class="link-button" type="button" @click="goLogin">返回登录</button>
      </div>
    </section>
  </main>
</template>
