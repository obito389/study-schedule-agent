package com.studyplanner.contract;

/**
 * 导入流程状态。初版至少覆盖以下六个状态。
 */
public enum ImportStatus {
    UPLOADED,
    PROCESSING,
    REVIEW,
    COMMITTED,
    FAILED,
    EXPIRED
}
