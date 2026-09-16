package com.studyplanner.agent;

import com.studyplanner.contract.ActionType;
import com.studyplanner.contract.ScheduleSnapshot;

import java.util.List;
import java.util.Map;

/**
 * 模型适配端口。外部模型能力经此隔离；初版提供 {@link MockModelAdapter}，
 * 保证没有模型密钥时仍可演示。Agent 只能基于决策生成 ActionSpec，
 * 不得直接访问 Repository。
 */
public interface ModelAdapter {

    Decision decide(String instruction, ScheduleSnapshot snapshot);

    /**
     * 模型决策。clarificationMessage 非空表示信息不足，应返回
     * NEEDS_CLARIFICATION，不生成动作。
     */
    record Decision(ActionType type, List<String> targets, Map<String, Object> changes,
                    String clarificationMessage) {

        public static Decision clarify(String message) {
            return new Decision(null, List.of(), Map.of(), message);
        }

        public static Decision action(ActionType type, List<String> targets, Map<String, Object> changes) {
            return new Decision(type, targets, changes, null);
        }
    }
}
