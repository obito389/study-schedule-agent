import { test, expect } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    const state = { starts: 0, stops: 0, aborts: 0, instance: null as any };
    (window as any).speechTest = state;
    (window as any).SpeechRecognition = class {
      onstart: any;
      onend: any;
      onresult: any;
      onerror: any;
      constructor() {
        state.instance = this;
      }
      start() {
        state.starts++;
        this.onstart?.();
      }
      stop() {
        state.stops++;
        this.onend?.();
      }
      abort() {
        state.aborts++;
        this.onend?.();
      }
    };
  });
  await page.goto("/#/ai");
  await expect(page.getByTestId("ai-instruction")).toBeVisible();
});

test("short click edits normally; hold captures Chinese, appends results, releases outside and cleans up", async ({
  page,
}) => {
  const input = page.getByTestId("ai-instruction");
  await input.fill("原有要求");
  await input.click();
  expect(await page.evaluate(() => (window as any).speechTest.starts)).toBe(0);
  await input.hover();
  await page.mouse.down();
  await expect(page.getByRole("status")).toContainText("正在听");
  await page.evaluate(() =>
    (window as any).speechTest.instance.onresult({
      results: [{ 0: { transcript: "明天晚上复习数据库" }, isFinal: false }],
    }),
  );
  await expect(input).toHaveValue("原有要求 明天晚上复习数据库");
  await page.evaluate(() =>
    (window as any).speechTest.instance.onresult({
      results: [
        { 0: { transcript: "明天晚上复习数据库一小时" }, isFinal: true },
      ],
    }),
  );
  await page.mouse.move(1, 1);
  await page.mouse.up();
  await expect(input).toHaveValue("原有要求 明天晚上复习数据库一小时");
  expect(await page.evaluate(() => (window as any).speechTest.stops)).toBe(1);
  await input.hover();
  await page.mouse.down();
  await expect(page.getByRole("status")).toContainText("正在听");
  await page
    .getByRole("link", { name: /学习日历/ })
    .first()
    .evaluate((el: HTMLElement) => el.click());
  expect(await page.evaluate(() => (window as any).speechTest.aborts)).toBe(1);
  await page.mouse.up();
});

test("unsupported browser and denied microphone give actionable feedback", async ({
  page,
}) => {
  const button = page.getByRole("button", { name: /按住说话/ });
  await button.focus();
  await page.keyboard.down("Space");
  await page.evaluate(() =>
    (window as any).speechTest.instance.onerror({ error: "not-allowed" }),
  );
  await page.keyboard.up("Space");
  await expect(page.getByRole("status")).toContainText("麦克风权限被拒绝");
  await page.evaluate(() => {
    (window as any).speechTest.instance.onend();
    (window as any).SpeechRecognition = undefined;
    (window as any).webkitSpeechRecognition = undefined;
  });
  await page.keyboard.press("Space");
  await expect(page.getByRole("status")).toContainText(
    "当前浏览器不支持语音输入",
  );
});

test("drag before hold does not open microphone", async ({ page }) => {
  await page.getByTestId("ai-instruction").hover();
  await page.mouse.down();
  await page.mouse.move(300, 400);
  await page.waitForTimeout(650);
  await page.mouse.up();
  expect(await page.evaluate(() => (window as any).speechTest.starts)).toBe(0);
});
