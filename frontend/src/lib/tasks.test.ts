import { describe, it, expect } from "vitest";
import { visibleTasks } from "./tasks";
import type { TaskSnapshot } from "../types/contracts";
const task = (
  id: string,
  title: string,
  priority: number,
  deadline: string,
  status: TaskSnapshot["status"] = "TODO",
): TaskSnapshot => ({
  id,
  title,
  priority,
  deadline,
  status,
  courseId: null,
  estimatedDurationMinutes: 60,
  scheduledBlocks: [],
});
const items = [
  task("later", "Read Vue", 5, "2026-10-21T00:00:00Z"),
  task("early", "数学", 2, "2026-10-20T00:00:00Z"),
  task("done", "Vue done", 4, "2026-10-19T00:00:00Z", "DONE"),
];
describe("task list controls", () => {
  it("normalizes whitespace and case while respecting active status", () => {
    expect(
      visibleTasks(items, "ACTIVE", " VUE ", "deadline").map((t) => t.id),
    ).toEqual(["later"]);
  });
  it("sorts deadline and priority without modifying snapshot order", () => {
    expect(
      visibleTasks(items, "ACTIVE", "", "deadline").map((t) => t.id),
    ).toEqual(["early", "later"]);
    expect(
      visibleTasks(items, "ACTIVE", "", "priority").map((t) => t.id),
    ).toEqual(["later", "early"]);
    expect(items.map((t) => t.id)).toEqual(["later", "early", "done"]);
  });
  it("distinguishes no matches from completed tasks", () => {
    expect(visibleTasks(items, "ALL", "不存在", "deadline")).toEqual([]);
    expect(
      visibleTasks(items, "DONE", "", "deadline").map((t) => t.id),
    ).toEqual(["done"]);
  });
});
