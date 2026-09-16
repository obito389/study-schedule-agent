package com.studyplanner.contract;

import java.util.List;

/**
 * 证据化用户事实。任何偏好都能说明来源、范围、强度、状态和样本证据。
 * version 用于使引用旧版本的 ActionSpec 草案失效。
 */
public record ProfileFact(
        int schemaVersion,
        String key,
        String value,
        String scope,
        double strength,
        String source,
        String state,
        List<String> evidenceIds,
        long version) {

    public ProfileFact {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("过期或非法契约版本: " + schemaVersion);
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("缺少必填字段: key");
        }
    }
}
