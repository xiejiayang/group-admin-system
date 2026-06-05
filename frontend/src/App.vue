<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const elementPlusLocale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    pagination: {
      ...zhCn.el.pagination,
      total: '总计 {total} 条'
    }
  }
}
</script>

<template>
  <el-config-provider :locale="elementPlusLocale">
    <RouterView />
  </el-config-provider>
</template>
