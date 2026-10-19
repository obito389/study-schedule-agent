import { test, expect } from "@playwright/test";

test("text import uses the provider and requires confirmation", async ({
  page,
  request,
}) => {
  const before = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  await page.goto("/#/import");
  await expect(
    page.getByText("文本 AI 识别 · 文件人工校对", { exact: true }),
  ).toBeVisible();
  await page.getByLabel("来源类型").selectOption("TEXT");
  await page
    .getByLabel("上传文件")
    .setInputFiles({
      name: "学习要求.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("明晚复习数据库一小时", "utf8"),
    });
  await page.getByRole("button", { name: "上传并生成提案" }).click();
  await expect(page.getByLabel("任务名称", { exact: true })).toHaveValue(
    "AI浏览器验收任务",
  );
  const pending = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(pending.tasks.length).toBe(before.tasks.length);
  await page
    .getByLabel("任务名称", { exact: true })
    .fill("团队整合 · 文本导入");
  await page.getByRole("button", { name: "已核对，确认创建任务" }).click();
  await expect(page.getByRole("heading", { name: "任务已保存" })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "任务已保存" })).toBeVisible();
  const after = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(
    after.tasks.filter(
      (t: { title: string }) => t.title === "团队整合 · 文本导入",
    ),
  ).toHaveLength(1);
});

test("failed text recognition never writes a task", async ({
  page,
  request,
}) => {
  const before = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  await page.goto("/#/import");
  await page.getByLabel("来源类型").selectOption("TEXT");
  await page
    .getByLabel("上传文件")
    .setInputFiles({
      name: "失败.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("模拟服务失败", "utf8"),
    });
  await page.getByRole("button", { name: "上传并生成提案" }).click();
  await expect(page.getByText("模型识别失败，请重试或手动填写")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "重试识别", exact: true }),
  ).toBeVisible();
  const after = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(after.tasks.length).toBe(before.tasks.length);
  expect(after.planningRevision).toBe(before.planningRevision);
});

test("atomic batch is read-only until applied and appears in task list", async ({
  page,
  request,
}) => {
  const before = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  const body = {
    operationKey: "browser-team-batch",
    baseRevision: before.planningRevision,
    actions: ["团队批次 · 任务一", "团队批次 · 任务二"].map((title) => ({
      type: "CREATE_TASK",
      payload: {
        title,
        deadline: new Date(Date.now() + 3 * 86400000).toISOString(),
        durationMinutes: 30,
        priority: 3,
      },
    })),
  };
  const preview = await request.post(
    "http://127.0.0.1:8081/api/v1/executor/preview",
    { data: body },
  );
  expect((await preview.json()).valid).toBe(true);
  const pending = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(pending.tasks.length).toBe(before.tasks.length);
  const receipt = await request.post(
    "http://127.0.0.1:8081/api/v1/executor/apply",
    { data: body },
  );
  expect(receipt.ok()).toBe(true);
  const first = await receipt.json();
  const retry = await request.post(
    "http://127.0.0.1:8081/api/v1/executor/apply",
    { data: body },
  );
  expect(await retry.json()).toEqual(first);
  await page.goto("/#/tasks");
  await expect(
    page.getByRole("cell", { name: "团队批次 · 任务一", exact: false }),
  ).toBeVisible();
  await expect(
    page.getByRole("cell", { name: "团队批次 · 任务二", exact: false }),
  ).toBeVisible();
});

test("stale plan can refresh both schedule and preview before retrying", async ({
  page,
  request,
}) => {
  await page.goto("/#/tasks");
  await page.getByTestId("generate-plan").click();
  await expect(page.getByRole("dialog")).toBeVisible();
  const snapshot = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  const change = await request.post("http://127.0.0.1:8081/api/v1/actions", {
    data: {
      operationKey: "browser-invalidate-preview",
      baseRevision: snapshot.planningRevision,
      type: "CREATE_TASK",
      authorizationScope: "schedule:write:self",
      payload: {
        title: "预览冲突验收",
        deadline: new Date(Date.now() + 3 * 86400000).toISOString(),
        durationMinutes: 30,
        priority: 3,
      },
    },
  });
  expect(change.ok()).toBe(true);
  await page.getByTestId("confirm-plan").click();
  await expect(page.locator(".conflict-banner")).toBeVisible();
  await page.getByRole("button", { name: "刷新并重试", exact: true }).click();
  await expect(page.locator(".conflict-banner")).toHaveCount(0);
  await expect(page.getByTestId("confirm-plan")).toBeEnabled();
  await page.getByRole("button", { name: "返回调整", exact: true }).click();
});
