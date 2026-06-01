import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  reporter: [['list']],
  timeout: 30_000,
  expect: {
    timeout: 10_000
  },
  use: {
    // 默认验证 Docker Compose 暴露的 Nginx 地址；本地联调可通过环境变量改为 Vite 地址。
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://127.0.0.1:8080',
    trace: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
})
