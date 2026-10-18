import { defineConfig } from "@playwright/test";
import { mkdirSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
const javaTmp = join(process.env.PUBLIC ?? tmpdir(), "studyplanner-java-tmp");
mkdirSync(javaTmp, { recursive: true });
const javaOptions =
  process.platform === "win32"
    ? `"-Djdk.net.unixdomain.tmpdir=${javaTmp}" `
    : "";
export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  workers: 1,
  timeout: 45000,
  use: {
    baseURL: "http://127.0.0.1:5174",
    headless: true,
    viewport: { width: 1440, height: 1040 },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: [
    {
      command: `java ${javaOptions}-jar ../backend/target/study-planner-backend-0.0.1-SNAPSHOT.jar --server.port=8081 "--spring.datasource.url=jdbc:h2:mem:e2e;MODE=MySQL;DB_CLOSE_DELAY=-1"`,
      url: "http://127.0.0.1:8081/api/v1/health",
      timeout: 120000,
      reuseExistingServer: false,
    },
    {
      command: "npm run dev -- --port 5174",
      url: "http://127.0.0.1:5174",
      env: { PLANNER_API_URL: "http://127.0.0.1:8081" },
      timeout: 60000,
      reuseExistingServer: false,
    },
  ],
});
