import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/final',
  fullyParallel: false,
  workers: 1,
  timeout: 30000,
  use: { baseURL: process.env.TEAM_E2E_URL ?? 'http://127.0.0.1:5182', extraHTTPHeaders: {'X-User-Id':'1'},
    viewport: { width: 1440, height: 1000 }, trace: 'retain-on-failure' },
})
