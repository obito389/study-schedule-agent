package com.studyplanner.contract;

import java.time.Instant;

/**
 * 时间块。用于固定占用、锁定块和排程候选块。
 * 初版时间槽粒度固定为 30 分钟，start/end 使用带时区的 Instant。
 */
public record TimeBlock(
        int schemaVersion,
        String id,
        Instant start,
        Instant end,
        boolean locked,
        String label) {
}
