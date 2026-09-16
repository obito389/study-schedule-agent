package com.studyplanner.contract;

import java.util.List;
import java.util.Map;

/**
 * 结构化动作。Agent 只能生成 ActionSpec，不得直接访问 Repository 或宣称写入成功。
 * operationKey 用于幂等；baseRevision 为乐观锁基准版本；
 * changes 描述变更内容；reasonFacts 引用用户事实 key。
 */
public record ActionSpec(
        int schemaVersion,
        String operationKey,
        long baseRevision,
        ActionType type,
        ActionStatus status,
        List<String> targets,
        Map<String, Object> changes,
        String authorizationScope,
        List<String> reasonFacts) {

    public ActionSpec {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("过期或非法契约版本: " + schemaVersion);
        }
        if (operationKey == null || operationKey.isBlank()) {
            throw new IllegalArgumentException("缺少必填字段: operationKey");
        }
    }
}
