<script setup lang="ts">
import { Lock, Right, User } from '@element-plus/icons-vue'
import {
  ElButton,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMessage,
  type FormInstance,
  type FormRules
} from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import gateUrl from '@/assets/group-gate.png'
import type { AuthUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const shellStyle = {
  backgroundImage: `linear-gradient(90deg, rgba(10, 20, 38, 0.72), rgba(10, 20, 38, 0.28)), url(${gateUrl})`
}

const landingPath = (user: AuthUser) => {
  const roleSet = new Set(user.roles)

  if (
    roleSet.has('GENERAL_ADMIN') ||
    roleSet.has('GENERAL_ADMIN_USER') ||
    user.departmentCode === 'GENERAL_ADMIN'
  ) {
    return '/general-admin'
  }

  return '/party-hr'
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)

  if (!valid) {
    return
  }

  loading.value = true

  try {
    const authStore = useAuthStore()
    const user = await authStore.login({
      username: form.username.trim(),
      password: form.password
    })

    await router?.push(landingPath(user))
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const goRegister = async () => {
  await router?.push('/register')
}
</script>

<template>
  <main class="auth-shell" :style="shellStyle">
    <section class="auth-panel" aria-label="登录">
      <div class="brand-block">
        <p class="brand-kicker">统一身份认证</p>
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
            autocomplete="current-password"
            :prefix-icon="Lock"
            placeholder="请输入密码"
            show-password
            size="large"
            type="password"
          />
        </el-form-item>

        <el-button
          class="auth-submit"
          :loading="loading"
          native-type="button"
          size="large"
          type="primary"
          @click="handleLogin"
        >
          <span>登录</span>
          <el-icon><Right /></el-icon>
        </el-button>
      </el-form>

      <div class="auth-extra">
        <span>没有账号？</span>
        <button class="link-button" type="button" @click="goRegister">注册账号</button>
      </div>
    </section>
  </main>
</template>
