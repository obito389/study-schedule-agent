package com.studyplanner.agent;

import com.studyplanner.contract.ActionSpec;
import com.studyplanner.contract.ActionStatus;

/**
 * Agent 处理结果。status 覆盖 NEEDS_CLARIFICATION / NEEDS_CONFIRMATION /
 * APPLIED / FAILED；信息不足或失败时 actionSpec 为 null。
 */
public record AgentResult(ActionStatus status, ActionSpec actionSpec, String message) {
}
