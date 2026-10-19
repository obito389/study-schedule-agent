// Inputs are the Beijing wall-clock strings already used by calendarEvents.
// Treat them as floating dates; the host computer's timezone must not change them.
export function calendarStartTime(
  events: { start: string; end: string }[],
  rangeStart: string,
  rangeEnd: string,
): string {
  let minutes = 360;
  const start = Date.parse(rangeStart + "T00:00:00Z");
  const end = Date.parse(rangeEnd + "T00:00:00Z");
  if (!Number.isFinite(start) || !Number.isFinite(end)) return "06:00:00";
  const day = 86400000;
  for (const event of events) {
    const from = Math.max(Date.parse(event.start + "Z"), start);
    const to = Math.min(Date.parse(event.end + "Z"), end);
    if (!(from < to)) continue;
    const midnight = Math.floor(from / day) * day;
    const firstMinute = (from - midnight) / 60000;
    // An event continuing past midnight occupies the following morning too.
    minutes = Math.min(minutes, to > midnight + day ? 0 : firstMinute);
  }
  minutes = Math.floor(minutes / 60) * 60;
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}:00`;
}
