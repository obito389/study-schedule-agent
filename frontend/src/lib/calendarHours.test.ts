import { describe, it, expect } from "vitest";
import { calendarStartTime } from "./calendarHours";
const range = ["2026-10-19", "2026-10-20"] as const;
describe("visible calendar hours", () => {
  it("defaults to 06:00 for empty or daytime schedules", () => {
    expect(calendarStartTime([], ...range)).toBe("06:00:00");
    expect(
      calendarStartTime(
        [{ start: "2026-10-19T08:00", end: "2026-10-19T09:00" }],
        ...range,
      ),
    ).toBe("06:00:00");
  });
  it("expands to the half-hour slot containing the earliest event", () => {
    expect(
      calendarStartTime(
        [{ start: "2026-10-19T04:45", end: "2026-10-19T07:00" }],
        ...range,
      ),
    ).toBe("04:30:00");
  });
  it("ignores events outside the displayed dates", () => {
    expect(
      calendarStartTime(
        [{ start: "2026-10-18T02:00", end: "2026-10-18T03:00" }],
        ...range,
      ),
    ).toBe("06:00:00");
  });
  it("includes the continuation of an overnight event", () => {
    expect(
      calendarStartTime(
        [{ start: "2026-10-18T23:00", end: "2026-10-19T02:00" }],
        ...range,
      ),
    ).toBe("00:00:00");
    expect(
      calendarStartTime(
        [{ start: "2026-10-19T23:00", end: "2026-10-20T02:00" }],
        "2026-10-19",
        "2026-10-26",
      ),
    ).toBe("00:00:00");
  });
  it("does not expand for an event ending exactly at midnight", () => {
    expect(
      calendarStartTime(
        [{ start: "2026-10-19T23:00", end: "2026-10-20T00:00" }],
        "2026-10-19",
        "2026-10-26",
      ),
    ).toBe("06:00:00");
  });
});
