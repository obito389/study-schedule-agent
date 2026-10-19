// Calendar events already contain Beijing wall-clock strings with no offset.
export function scheduledCalendarDays(
  events: { start: string; end: string }[],
  rangeStart: string,
  rangeEnd: string,
): Set<string> {
  const result = new Set<string>();
  const start = Date.parse(rangeStart + "T00:00:00Z");
  const end = Date.parse(rangeEnd + "T00:00:00Z");
  if (!Number.isFinite(start) || !Number.isFinite(end)) return result;
  const intervals = events.map((e) => [
    Date.parse(e.start + "Z"),
    Date.parse(e.end + "Z"),
  ]);
  for (let day = start; day < end; day += 86400000) {
    if (
      intervals.some(
        ([from, to]) => from < to && from < day + 86400000 && to > day,
      )
    ) {
      result.add(new Date(day).toISOString().slice(0, 10));
    }
  }
  return result;
}

// FullCalendar's date is a local floating date, matching its timeZone: local.
export function calendarDayKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
