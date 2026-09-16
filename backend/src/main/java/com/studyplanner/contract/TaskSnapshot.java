package com.studyplanner.contract;

import java.time.Instant;

/**
 * 任务快照。courseId 可空（无课程归属的任务）；priority 越大越优先；
 * scheduledBlock 为已排入的时间块，未排入时为 null。
 */
public record TaskSnapshot(
        int schemaVersion,
        String id,
        String title,
        String courseId,
        Instant deadline,
        int durationMinutes,
        int priority,
        String state,
        TimeBlock scheduledBlock) {
}
