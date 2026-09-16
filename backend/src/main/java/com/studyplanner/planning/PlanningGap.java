package com.studyplanner.planning;

/**
 * 未排入任务及原因。禁止悄悄截短任务：无法完整排入时必须返回缺口。
 */
public record PlanningGap(String taskId, String reason) {
}
