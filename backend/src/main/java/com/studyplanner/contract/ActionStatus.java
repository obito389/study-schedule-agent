package com.studyplanner.contract;

/**
 * 动作生命周期状态。影响其他任务或多对象的动作应进入 NEEDS_CONFIRMATION，
 * 信息不足进入 NEEDS_CLARIFICATION，经事务执行器校验提交后转为 APPLIED。
 */
public enum ActionStatus {
    PENDING,
    NEEDS_CLARIFICATION,
    NEEDS_CONFIRMATION,
    APPLIED,
    FAILED,
    CANCELLED
}
