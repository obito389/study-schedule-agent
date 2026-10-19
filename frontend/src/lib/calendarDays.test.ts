import { describe, it, expect } from "vitest";
import { scheduledCalendarDays, calendarDayKey } from "./calendarDays";
describe("month calendar date markers", () => {
  it("only marks days with scheduled blocks, not empty or invalid intervals", () => {
    expect([
      ...scheduledCalendarDays(
        [
          { start: "2026-10-19T17:30", end: "2026-10-19T18:30" },
          { start: "2026-10-20T18:00", end: "2026-10-20T18:00" },
        ],
        "2026-10-19",
        "2026-10-22",
      ),
    ]).toEqual(["2026-10-19"]);
  });
  it("marks both days for overnight blocks but excludes a midnight end", () => {
    expect([
      ...scheduledCalendarDays(
        [{ start: "2026-10-19T23:00", end: "2026-10-21T00:00" }],
        "2026-10-19",
        "2026-10-23",
      ),
    ]).toEqual(["2026-10-19", "2026-10-20"]);
  });
  it("clips overnight events to the displayed month range", () => {
    expect([
      ...scheduledCalendarDays(
        [{ start: "2026-10-18T23:00", end: "2026-10-19T02:00" }],
        "2026-10-19",
        "2026-10-20",
      ),
    ]).toEqual(["2026-10-19"]);
  });
  it("matches floating calendar dates without converting to UTC", () => {
    expect(calendarDayKey(new Date(2026, 9, 19))).toBe("2026-10-19");
  });
});
