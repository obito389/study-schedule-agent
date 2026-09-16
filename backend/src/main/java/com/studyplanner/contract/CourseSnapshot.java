package com.studyplanner.contract;

import java.util.List;

/**
 * 课程快照。weeklyTimeBlocks 表达课程固定的每周上课时段。
 */
public record CourseSnapshot(
        int schemaVersion,
        String id,
        String code,
        String title,
        List<TimeBlock> weeklyTimeBlocks) {
}
