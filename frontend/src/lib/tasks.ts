import type { TaskSnapshot } from "../types/contracts";

export function visibleTasks(
  tasks: TaskSnapshot[],
  filter: string,
  query: string,
  sort: string,
) {
  const needle = query.trim().toLocaleLowerCase();
  return tasks
    .filter((task) => {
      const matchesStatus =
        filter === "ALL" ||
        (filter === "ACTIVE"
          ? !["DONE", "CANCELLED"].includes(task.status)
          : task.status === filter);
      return matchesStatus && task.title.toLocaleLowerCase().includes(needle);
    })
    .sort((a, b) => {
      if (sort === "priority" && a.priority !== b.priority)
        return b.priority - a.priority;
      return (
        Date.parse(a.deadline) - Date.parse(b.deadline) ||
        a.id.localeCompare(b.id)
      );
    });
}
