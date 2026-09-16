package com.studyplanner.planning;

/**
 * 硬约束违反项。code 为稳定错误码，objectId 为涉及对象（任务/时间块），
 * message 为人类可读说明。
 */
public record ConstraintViolation(String code, String objectId, String message) {
}
