<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import {
  ElButton,
  ElCard,
  ElEmpty,
  ElMessage,
  ElSkeleton,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { fetchOperationLogs, type OperationLog } from '@/api/system'

const logs = ref<OperationLog[]>([])
const loading = ref(false)
const hasLogs = computed(() => logs.value.length > 0)

const toMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message : fallback
}

const loadLogs = async () => {
  loading.value = true

  try {
    // 操作记录是后台审计入口，列表刷新必须始终以服务端最新结果为准。
    logs.value = await fetchOperationLogs()
  } catch (error) {
    const message = toMessage(error, '操作记录加载失败')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadLogs()
})
</script>

<template>
  <section class="settings-page">
    <div class="page-heading">
      <div>
        <h1>操作记录</h1>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadLogs">刷新</el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <el-skeleton v-if="loading && !hasLogs" class="table-skeleton" animated :rows="6" />
      <el-table v-else-if="hasLogs" :data="logs" row-key="id">
        <el-table-column label="操作账号" min-width="140" prop="operatorUsername" show-overflow-tooltip />
        <el-table-column label="姓名" min-width="120" prop="realName" show-overflow-tooltip />
        <el-table-column label="部门" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.department || '未分配部门' }}
          </template>
        </el-table-column>
        <el-table-column label="手机号" min-width="140" prop="phone" show-overflow-tooltip />
        <el-table-column label="角色" min-width="150" prop="role" show-overflow-tooltip />
        <el-table-column label="操作记录" min-width="260" prop="operationRecord" show-overflow-tooltip />
      </el-table>

      <el-empty v-else description="暂无操作记录" />
    </el-card>
  </section>
</template>
