import { test, expect } from "@playwright/test";

test("day, week and month calendars expand fully without internal scrolling", async ({
  page,
}) => {
  for (const width of [444, 671, 1440]) {
    await page.setViewportSize({ width, height: 706 });
    await page.goto("/#/");
    for (const view of ["timeGridDay", "timeGridWeek", "dayGridMonth"]) {
      await page.locator(`.fc-${view}-button`).click();
      await expect(page.locator(`.fc-${view}-view`)).toBeVisible();
      await expect
        .poll(() =>
          page
            .locator(".fc-scroller")
            .evaluateAll((elements) =>
              elements.every(
                (el) =>
                  el.scrollHeight <= el.clientHeight + 1 &&
                  el.scrollWidth <= el.clientWidth + 1,
              ),
            ),
        )
        .toBe(true);
      if (view !== "dayGridMonth") {
        await expect(
          page.locator(".fc-timegrid-slot-label-cushion").first(),
        ).toHaveText("6时");
        await expect(
          page.locator(".fc-timegrid-slot-label-cushion").last(),
        ).toHaveText("23时");
      }
    }
  }
});

test("hovering the weekly grid does not cover hour separators", async ({
  page,
}) => {
  await page.setViewportSize({ width: 671, height: 706 });
  await page.goto("/#/");
  await page.locator(".fc-timeGridWeek-button").click();
  await page.locator(".fc-timegrid-col.fc-day").nth(2).hover();
  expect(
    await page
      .locator(".fc-timegrid-cols tbody tr")
      .first()
      .evaluate((el) => getComputedStyle(el).backgroundColor),
  ).toBe("rgba(0, 0, 0, 0)");
  await page.screenshot({
    path: "test-results/calendar-grid-hover.png",
    fullPage: true,
  });
});

test("calendar has readable event columns and day/week/month navigation on mobile", async ({
  page,
}) => {
  await page.setViewportSize({ width: 444, height: 706 });
  await page.goto("/#/");
  await expect(page.locator(".fc-timeGridDay-view")).toBeVisible();
  await expect(
    page.locator(".fc-timegrid-slot-label-cushion").first(),
  ).toHaveText("6时");
  await expect(
    page.locator(
      ".fc-timegrid-now-indicator-line,.fc-timegrid-now-indicator-arrow",
    ),
  ).toHaveCount(0);
  await page.locator(".fc-timeGridWeek-button").click();
  await expect(page.locator(".fc-col-header-cell")).toHaveCount(5);
  const weekdayStart = await page
    .locator(".fc-col-header-cell")
    .first()
    .getAttribute("data-date");
  await expect
    .poll(() =>
      page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    )
    .toBe(true);
  const event = page.locator(".calendar-event-entry").first();
  await expect(event).toBeVisible();
  expect(
    await event.evaluate((el) => {
      const time = el
        .querySelector(".calendar-event-time")!
        .getBoundingClientRect();
      const title = el
        .querySelector(".calendar-event-title")!
        .getBoundingClientRect();
      return time.right < title.left;
    }),
  ).toBe(true);
  const eventStyle = await event.evaluate((el) => {
    const times = el.querySelectorAll("time");
    const card = el.closest(".fc-timegrid-event")!;
    return {
      firstSize: getComputedStyle(times[0]!).fontSize,
      lastSize: getComputedStyle(times[1]!).fontSize,
      shadow: getComputedStyle(card).boxShadow,
      separator: el.querySelector(".calendar-event-separator")?.textContent,
    };
  });
  expect(eventStyle.firstSize).toBe(eventStyle.lastSize);
  expect(eventStyle.shadow).toBe("none");
  expect(eventStyle.separator?.trim()).toBe("—");
  for (const width of [444, 671, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    await expect
      .poll(() =>
        page.evaluate(() => {
          const heads = [...document.querySelectorAll(".fc-col-header-cell")];
          const cols = [
            ...document.querySelectorAll(".fc-timegrid-col.fc-day"),
          ];
          const pane = document.querySelector(".fc-view-harness")!;
          return (
            heads.length === 5 &&
            heads.every((head, index) => {
              const h = head.getBoundingClientRect(),
                c = cols[index]!.getBoundingClientRect();
              return (
                Math.abs(h.left - c.left) < 1 && Math.abs(h.right - c.right) < 1
              );
            }) &&
            pane.scrollWidth <= pane.clientWidth + 1
          );
        }),
      )
      .toBe(true);
    expect(
      await page
        .locator(".fc-timegrid-event")
        .first()
        .evaluate((el) => {
          const card = el.getBoundingClientRect();
          const col = el.closest(".fc-timegrid-col")!.getBoundingClientRect();
          return (
            Math.abs(card.left - col.left) <= 1.5 &&
            Math.abs(card.right - col.right) <= 1.5
          );
        }),
    ).toBe(true);
  }
  await page.setViewportSize({ width: 671, height: 706 });
  await page.screenshot({
    path: "test-results/calendar-week-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "周末", exact: true }).click();
  await expect(page.locator(".fc-col-header-cell")).toHaveCount(2);
  await expect(page.locator(".fc-col-header-cell").first()).toContainText(
    "周六",
  );
  await expect(page.locator(".fc-col-header-cell").last()).toContainText(
    "周日",
  );
  const saturday = await page
    .locator(".fc-col-header-cell")
    .first()
    .getAttribute("data-date");
  await page.locator(".fc-next-button").click();
  await expect
    .poll(
      async () =>
        Date.parse(
          (await page
            .locator(".fc-col-header-cell")
            .first()
            .getAttribute("data-date"))!,
        ) - Date.parse(saturday!),
    )
    .toBe(7 * 86400000);
  await page.locator(".fc-prev-button").click();
  await page.getByRole("button", { name: "工作日", exact: true }).click();
  await expect(page.locator(".fc-col-header-cell")).toHaveCount(5);
  await expect(page.locator(".fc-col-header-cell").first()).toHaveAttribute(
    "data-date",
    weekdayStart!,
  );
  await page.locator(".fc-dayGridMonth-button").click();
  await expect(page.locator(".fc-dayGridMonth-view")).toBeVisible();
  await page.locator(".fc-timeGridDay-button").click();
  await expect(page.locator(".fc-timeGridDay-view")).toBeVisible();
});

test("month hides event details and saves date notes across reload without changing schedule", async ({
  page,
  request,
}) => {
  await page.setViewportSize({ width: 444, height: 706 });
  const before = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  await page.goto("/#/");
  await page.locator(".fc-dayGridMonth-button").click();
  await expect(page.locator(".fc-dayGridMonth-view .fc-event")).toHaveCount(0);
  await expect(
    page.locator(".month-day-button.has-day-content").first(),
  ).toBeVisible();
  const emptyDay = page
    .locator(
      ".fc-daygrid-day:not(.fc-day-other) .month-day-button:not(.has-day-content)",
    )
    .first();
  const day = await emptyDay.getAttribute("data-date");
  await emptyDay.click();
  await page.getByTestId("day-note-input").fill("记得准备答辩材料");
  await page.getByTestId("save-day-note").click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  const dayButton = page.locator(`.month-day-button[data-date="${day}"]`);
  await expect(dayButton).toHaveClass(/has-day-content/);
  await expect(dayButton).not.toContainText("答辩材料");
  await page.reload();
  await page.locator(".fc-dayGridMonth-button").click();
  await dayButton.click();
  await expect(page.getByTestId("day-note-input")).toHaveValue(
    "记得准备答辩材料",
  );
  await page.getByTestId("day-note-input").fill("");
  await page.getByTestId("save-day-note").click();
  await expect(dayButton).not.toHaveClass(/has-day-content/);
  const after = await (
    await request.get("http://127.0.0.1:8081/api/v1/schedule")
  ).json();
  expect(after.planningRevision).toBe(before.planningRevision);
  expect(after.tasks).toEqual(before.tasks);
  await page.screenshot({
    path: "test-results/calendar-month-overview.png",
    fullPage: true,
  });
});

test("failed note storage keeps the draft and does not mark the date as saved", async ({
  page,
}) => {
  await page.addInitScript(() => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      if (key.startsWith("study-planner:day-note:"))
        throw new DOMException("Storage full", "QuotaExceededError");
      original.call(this, key, value);
    };
  });
  await page.goto("/#/");
  await page.locator(".fc-dayGridMonth-button").click();
  await page.locator(".month-day-button").first().click();
  await page.getByTestId("day-note-input").fill("不能丢失的待保存内容");
  await page.getByTestId("save-day-note").click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.getByRole("alert")).toContainText("保存失败");
  await expect(page.getByTestId("day-note-input")).toHaveValue(
    "不能丢失的待保存内容",
  );
});

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
