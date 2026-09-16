package com.studyplanner.contract;

import java.time.Instant;
import java.util.List;

/**
 * 多模态导入的校对结果。由 importing 模块产生，供 Agent 和排程器消费。
 * requestKey 用于幂等，重复请求返回原结果，不重复写入。
 */
public record ImportProposal(
        int schemaVersion,
        String requestKey,
        String kind,
        String title,
        String courseCandidate,
        Instant deadline,
        int durationMinutes,
        List<String> sourceEvidence,
        List<String> ambiguities) {

    public ImportProposal {
        requireVersion(schemaVersion);
        requireText(requestKey, "requestKey");
        requireText(kind, "kind");
        requireText(title, "title");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少必填字段: " + name);
        }
    }

    private static void requireVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("过期或非法契约版本: " + version);
        }
    }
}
