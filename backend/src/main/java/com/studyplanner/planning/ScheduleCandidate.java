package com.studyplanner.planning;

import com.studyplanner.contract.TimeBlock;

import java.util.List;

/**
 * 排程候选结果。blocks 为排入的时间块（id 对应任务 id），gaps 为未排入任务及原因。
 * 排程器只生成候选结果，不写数据库。
 */
public record ScheduleCandidate(List<TimeBlock> blocks, List<PlanningGap> gaps) {

    public static ScheduleCandidate empty() {
        return new ScheduleCandidate(List.of(), List.of());
    }
}
