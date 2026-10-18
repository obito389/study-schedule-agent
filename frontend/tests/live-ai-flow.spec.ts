import { test, expect } from "@playwright/test";
test("AI interpretation uses backend adapter, persists draft, confirms once, clarifies and reports failure", async ({
  page,
  request,
}) => {
  const before = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  await page.goto("/#/ai");
  await page
    .getByTestId("ai-instruction")
    .fill("创建明晚8点复习数据库一个小时的任务");
  await page.getByTestId("ai-generate").click();
  await expect(
    page.getByRole("heading", { name: "AI浏览器验收任务" }),
  ).toBeVisible();
  await expect(
    page.getByText("测试提供方解析了自然语言中的时段和耗时"),
  ).toBeVisible();
  const preview = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(preview.tasks.length).toBe(before.tasks.length);
  await page.screenshot({
    path: "test-results/ai-test-provider-preview.png",
    fullPage: true,
  });
  await page.getByTestId("ai-confirm").click();
  await expect(page.getByTestId("ai-saved")).toBeVisible();
  const after = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(after.tasks.length).toBe(before.tasks.length + 1);
  expect(
    after.tasks.find((t: { title: string }) => t.title === "AI浏览器验收任务")
      .scheduledBlocks,
  ).toHaveLength(1);
  await page.getByTestId("ai-instruction").fill("缺少必要信息的请求");
  await page.getByTestId("ai-generate").click();
  await expect(
    page.getByText("请补充预计耗时和截止时间", { exact: true }),
  ).toBeVisible();
  await expect(page.getByTestId("ai-confirm")).toHaveCount(0);
  await page.getByTestId("ai-instruction").fill("模拟服务失败");
  await page.getByTestId("ai-generate").click();
  await expect(page.getByTestId("ai-error")).toContainText("AI 服务暂时不可用");
  await expect(page.getByTestId("ai-confirm")).toHaveCount(0);
});
