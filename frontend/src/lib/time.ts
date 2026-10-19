import type { ScheduleSnapshot } from "../types/contracts";
export function shanghaiInput(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  return new Date(d.getTime() + 8 * 3600000).toISOString().slice(0, 16);
}
export function toInstant(value: string): string {
  if (!value) throw new Error("请选择日期和时间");
  const d = new Date(value + "+08:00");
  if (Number.isNaN(d.getTime())) throw new Error("日期时间无效");
  return d.toISOString();
}
export function formatTime(iso: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(iso));
}
export function nextSlot(): string {
  return shanghaiInput(
    new Date(Math.ceil((Date.now() + 60000) / 1800000) * 1800000).toISOString(),
  );
}
export function tomorrowDeadline(): string {
  const d = new Date(Date.now() + 86400000);
  return shanghaiInput(d.toISOString()).slice(0, 10) + "T22:00";
}
export function calendarEvents(s: ScheduleSnapshot) {
  return [
    ...s.courses.flatMap((c) =>
      c.meetingBlocks.map((b) => ({
        id: b.id,
        title: c.name,
        start: shanghaiInput(b.start),
        end: shanghaiInput(b.end),
        color: "#ad8d59",
        extendedProps: { kind: "课程", block: b },
      })),
    ),
    ...s.tasks.flatMap((t) =>
      t.scheduledBlocks.map((b) => ({
        id: b.id,
        title: t.title,
        start: shanghaiInput(b.start),
        end: shanghaiInput(b.end),
        color: "#557cda",
        extendedProps: { kind: "任务", block: b, task: t },
      })),
    ),
    ...s.lockedBlocks.map((b) => ({
      id: b.id,
      title: b.ownerId,
      start: shanghaiInput(b.start),
      end: shanghaiInput(b.end),
      color: "#88958f",
      extendedProps: {
        kind: b.kind === "UNAVAILABLE" ? "禁止时段" : "锁定时段",
        block: b,
      },
    })),
  ];
}
