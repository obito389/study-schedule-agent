package com.studyplanner.contract;

import java.time.Instant;
import java.util.List;

/**
 * 排程快照。Agent 读取本快照生成动作；排程器和动作执行器共用。
 * planningRevision 用于乐观并发控制，baseRevision 校验据此判定版本冲突。
 */
public record ScheduleSnapshot(
        int schemaVersion,
        Instant now,
        String timezone,
        List<CourseSnapshot> courses,
        List<TaskSnapshot> tasks,
        List<TimeBlock> lockedBlocks,
        List<ProfileFact> activeFacts,
        long planningRevision) {

    public ScheduleSnapshot {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("过期或非法契约版本: " + schemaVersion);
        }
    }
}
