package com.studyplanner.enums;

/**
 * Agent动作状态
 * UNDONE：该动作已被撤销（第 4 周撤销机制），原动作保留但标记为 UNDONE，便于追溯完整链路。
 */
public enum ActionStatus {
    PREVIEW,
    CONFIRMED,
    APPLIED,
    FAILED,
    CANCELLED,
    UNDONE
}
