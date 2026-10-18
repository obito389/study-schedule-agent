import { describe, it, expect } from "vitest";
import { shanghaiInput, toInstant, calendarEvents, formatTime } from "./time";
import type { ScheduleSnapshot } from "../types/contracts";
describe("Shanghai time and calendar handoff", () => {
  it("converts UTC to input time without depending on browser timezone", () => {
    expect(shanghaiInput("2026-10-18T16:30:00Z")).toBe("2026-10-19T00:30");
  });
  it("converts a Shanghai form input back into an instant", () => {
    expect(toInstant("2026-10-19T00:30")).toBe("2026-10-18T16:30:00.000Z");
  });
  it("rejects missing and invalid dates", () => {
    expect(() => toInstant("")).toThrow();
    expect(() => toInstant("no-date")).toThrow();
  });
  it("formats a stable local date and time", () => {
    expect(formatTime("2026-10-18T16:30:00Z")).toContain("10/19");
    expect(formatTime("2026-10-18T16:30:00Z")).toContain("00:30");
  });
  it("maps courses, tasks and blocked periods without mixing their identities", () => {
    const start = "2026-10-19T00:00:00Z",
      end = "2026-10-19T00:30:00Z";
    const s: ScheduleSnapshot = {
      schemaVersion: 1,
      now: start,
      timezone: "Asia/Shanghai",
      planningRevision: 0,
      activeFacts: [],
      courses: [
        {
          id: "course",
          name: "数学",
          meetingBlocks: [
            {
              id: "c",
              ownerId: "course",
              start,
              end,
              locked: true,
              kind: "COURSE",
            },
          ],
        },
      ],
      tasks: [
        {
          id: "task",
          courseId: null,
          title: "作业",
          deadline: end,
          estimatedDurationMinutes: 30,
          priority: 3,
          status: "TODO",
          scheduledBlocks: [
            {
              id: "t",
              ownerId: "task",
              start,
              end,
              locked: false,
              kind: "TASK",
            },
          ],
        },
      ],
      lockedBlocks: [
        {
          id: "u",
          ownerId: "休息",
          start,
          end,
          locked: true,
          kind: "UNAVAILABLE",
        },
      ],
    };
    const events = calendarEvents(s);
    expect(events.map((e) => e.id)).toEqual(["c", "t", "u"]);
    expect(events.map((e) => e.extendedProps.kind)).toEqual([
      "课程",
      "任务",
      "禁止时段",
    ]);
    expect(events[0].start).toBe("2026-10-19T08:00");
  });
});
