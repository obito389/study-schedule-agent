import { test, expect } from "@playwright/test";

test("task search shows accurate empty state and clears back to actual tasks", async ({
  page,
}) => {
  await page.goto("/#/tasks");
  await page
    .getByLabel("搜索任务", { exact: true })
    .fill("没有匹配的独特任务名");
  await expect(
    page.getByRole("heading", { name: "这里暂时没有匹配的任务" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "新建第一个任务" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "清除筛选" }).click();
  await expect(page.getByLabel("搜索任务", { exact: true })).toHaveValue("");
  await expect(
    page.getByRole("button", { name: "全部", exact: true }),
  ).toHaveAttribute("aria-pressed", "true");
  await page.getByLabel("任务排序").selectOption("priority");
  await page.setViewportSize({ width: 444, height: 706 });
  await expect(page.locator(".tasks-panel tbody")).toBeVisible();
  await expect
    .poll(() =>
      page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    )
    .toBe(true);
  await page.screenshot({
    path: "test-results/workspace-tasks-mobile.png",
    fullPage: true,
  });
});

test("all pages fit narrow and desktop workspaces; profile stays compact", async ({
  page,
}) => {
  for (const width of [390, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    for (const route of ["ai", "tasks", "import", "profile", ""]) {
      await page.goto("/#/" + route);
      await expect(page.locator("main .panel").first()).toBeVisible();
      await expect
        .poll(() =>
          page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        )
        .toBe(true);
      await expect(
        page.getByRole("button", { name: "设置", exact: true }),
      ).toBeVisible();
      if (route === "profile")
        await expect(page.locator(".page-heading")).toHaveCount(0);
      await page.screenshot({
        path: `test-results/workspace-${route || "calendar"}-${width}.png`,
        fullPage: true,
      });
    }
  }
});
