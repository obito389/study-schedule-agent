package com.studyplanner.planning;

import java.time.Instant;

/**
 * 候选时间块。排程器为某个任务提议的时间区间，交给
 * {@link HardConstraintValidator} 做共用校验。
 */
public record CandidateBlock(String taskId, Instant start, Instant end) {

    public int durationMinutes() {
        return (int) (end.getEpochSecond() - start.getEpochSecond()) / 60;
    }
}
