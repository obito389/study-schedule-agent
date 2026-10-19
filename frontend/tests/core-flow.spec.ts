import { test, expect } from "@playwright/test";
test("task creation, planning confirmation, calendar, undo and completion", async ({
  page,
  request,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/#/tasks");
  await page.getByTestId("new-task").click();
  await page.getByTestId("task-title").fill("浏览器验收 · 数据库报告");
  await page.getByTestId("task-submit").click();
  await expect(
    page.getByRole("cell", { name: "浏览器验收 · 数据库报告", exact: false }),
  ).toBeVisible();
  await page.getByTestId("generate-plan").click();
  await expect(page.getByRole("dialog")).toContainText("尚未写入日历");
  await page.getByTestId("confirm-plan").click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await expect(page.getByText("已安排", { exact: true })).toBeVisible();
  await page.getByRole("link", { name: "学习日历" }).click();
  await expect(page.locator(".fc")).toBeVisible();
  await expect(page.locator(".fc-event")).toHaveCount(2);
  await page.locator(".fc-event").first().scrollIntoViewIfNeeded();
  await expect(page.locator(".fc-event").first()).toBeInViewport();
  await expect(page.locator(".el-message")).toHaveCount(0);
  await page.screenshot({
    path: "test-results/calendar-desktop.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "执行记录" }).click();
  const plan = page
    .locator(".history-item")
    .filter({ has: page.getByText("保存排程", { exact: true }) });
  await plan.getByRole("button", { name: "撤销这次操作" }).click();
  await page.getByRole("button", { name: "确认撤销", exact: true }).click();
  await expect(page.locator(".fc-event")).toHaveCount(0);
  await page.keyboard.press("Escape");
  await page.getByRole("link", { name: "任务清单" }).click();
  await page.getByRole("button", { name: "完成", exact: true }).click();
  await page.getByRole("button", { name: "确认完成", exact: true }).click();
  await page.getByRole("button", { name: "已完成", exact: true }).click();
  await expect(
    page.getByText("浏览器验收 · 数据库报告", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "设置", exact: true }).click();
  await page.getByRole("menuitem", { name: /偏好与反馈/ }).click();
  await expect(page.getByText("任务完成反馈", { exact: true })).toBeVisible();
  const snapshot = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(snapshot.tasks[0].status).toBe("DONE");
  expect(errors).toEqual([]);
});
test("mock import requires correction and saves a real task; mobile fits screen", async ({
  page,
  request,
}) => {
  await page.goto("/#/import");
  await page.getByLabel("上传文件").setInputFiles({
    name: "demo.png",
    mimeType: "image/png",
    buffer: Buffer.from([1, 2, 3]),
  });
  await page.getByRole("button", { name: "上传并生成提案" }).click();
  await expect(page.getByText("待校对", { exact: true })).toBeVisible();
  await page
    .getByLabel("任务名称", { exact: true })
    .fill("浏览器验收 · 导入校对任务");
  await page.getByRole("button", { name: "已核对，确认创建任务" }).click();
  await expect(page.getByRole("heading", { name: "任务已保存" })).toBeVisible();
  await page.getByRole("button", { name: "设置", exact: true }).click();
  await page.getByRole("menuitem", { name: /偏好与反馈/ }).click();
  await page.getByTestId("daily-limit").fill("180");
  await page.getByTestId("save-preference").click();
  await expect(
    page.getByText("每日最多 180 分钟", { exact: true }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("link", { name: "学习日历" }).click();
  await expect(page.locator(".fc")).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "test-results/calendar-mobile.png",
    fullPage: true,
  });
  const snapshot = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(
    snapshot.tasks.some(
      (t: { title: string }) => t.title === "浏览器验收 · 导入校对任务",
    ),
  ).toBe(true);
});
