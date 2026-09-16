package com.studyplanner.planning;

import com.studyplanner.contract.CourseSnapshot;
import com.studyplanner.contract.ProfileFact;
import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.TimeBlock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 排程与执行共用的唯一硬约束实现。
 *
 * 覆盖规则：截止时间、时间重叠、锁定时间块（课程固定时段与锁定块视为禁止占用）、
 * 每日上限（读取用户事实 key=daily.cap.minutes，0 表示无限制）。
 * 违反项按固定顺序返回：范围 → 截止 → 锁定块 → 与其他任务重叠 → 每日上限，
 * 相同输入始终返回相同顺序，满足确定性要求。
 */
public class HardConstraintValidator {

    public static final String INVALID_RANGE = "INVALID_RANGE";
    public static final String DEADLINE_EXCEEDED = "DEADLINE_EXCEEDED";
    public static final String LOCKED_BLOCK = "LOCKED_BLOCK";
    public static final String OVERLAP = "OVERLAP";
    public static final String DAILY_CAP_EXCEEDED = "DAILY_CAP_EXCEEDED";

    public static final String DAILY_CAP_KEY = "daily.cap.minutes";

    public List<ConstraintViolation> validate(ScheduleSnapshot snapshot, CandidateBlock candidate) {
        List<ConstraintViolation> violations = new ArrayList<>();

        if (!candidate.start().isBefore(candidate.end())) {
            violations.add(new ConstraintViolation(INVALID_RANGE, candidate.taskId(),
                    "候选时间块开始时间必须早于结束时间"));
            return violations;
        }

        TaskSnapshot task = findTask(snapshot, candidate.taskId());
        if (task != null && task.deadline() != null && candidate.end().isAfter(task.deadline())) {
            violations.add(new ConstraintViolation(DEADLINE_EXCEEDED, candidate.taskId(),
                    "任务结束时间超过截止时间（截止 " + task.deadline() + "）"));
        }

        for (TimeBlock locked : allLockedBlocks(snapshot)) {
            if (overlaps(candidate.start(), candidate.end(), locked.start(), locked.end())) {
                violations.add(new ConstraintViolation(LOCKED_BLOCK, candidate.taskId(),
                        "与锁定时间块冲突: " + safeLabel(locked)));
                break;
            }
        }

        for (TaskSnapshot other : snapshot.tasks()) {
            if (other.id().equals(candidate.taskId())) {
                continue;
            }
            TimeBlock otherBlock = other.scheduledBlock();
            if (otherBlock != null && overlaps(candidate.start(), candidate.end(), otherBlock.start(), otherBlock.end())) {
                violations.add(new ConstraintViolation(OVERLAP, candidate.taskId(),
                        "与任务 " + other.id() + " 已排时间块重叠"));
            }
        }

        int dailyCap = dailyCapMinutes(snapshot);
        if (dailyCap > 0) {
            ZoneId zone = ZoneId.of(snapshot.timezone());
            LocalDate date = candidate.start().atZone(zone).toLocalDate();
            long used = minutesOccupiedOnDate(snapshot, date, zone);
            if (used + candidate.durationMinutes() > dailyCap) {
                violations.add(new ConstraintViolation(DAILY_CAP_EXCEEDED, candidate.taskId(),
                        "当日已占用 " + used + " 分钟，超出每日上限 " + dailyCap + " 分钟"));
            }
        }

        return violations;
    }

    private TaskSnapshot findTask(ScheduleSnapshot snapshot, String taskId) {
        for (TaskSnapshot task : snapshot.tasks()) {
            if (task.id().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    /** 课程固定时段 + 锁定块，均为不可占用时间。 */
    private List<TimeBlock> allLockedBlocks(ScheduleSnapshot snapshot) {
        List<TimeBlock> blocks = new ArrayList<>(snapshot.lockedBlocks());
        for (CourseSnapshot course : snapshot.courses()) {
            blocks.addAll(course.weeklyTimeBlocks());
        }
        return blocks;
    }

    private String safeLabel(TimeBlock block) {
        return block.label() == null || block.label().isBlank() ? block.id() : block.label();
    }

    private boolean overlaps(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private int dailyCapMinutes(ScheduleSnapshot snapshot) {
        for (ProfileFact fact : snapshot.activeFacts()) {
            if (DAILY_CAP_KEY.equals(fact.key())) {
                try {
                    return Integer.parseInt(fact.value().trim());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /** 统计指定日期当天被课程、锁定块和其他任务占用（含候选自身当天部分由调用方另行计入）的分钟数。 */
    private long minutesOccupiedOnDate(ScheduleSnapshot snapshot, LocalDate date, ZoneId zone) {
        long total = 0;
        for (TimeBlock block : allLockedBlocks(snapshot)) {
            total += overlapMinutesOnDate(block.start(), block.end(), date, zone);
        }
        for (TaskSnapshot task : snapshot.tasks()) {
            if (task.scheduledBlock() != null) {
                total += overlapMinutesOnDate(task.scheduledBlock().start(), task.scheduledBlock().end(), date, zone);
            }
        }
        return total;
    }

    private long overlapMinutesOnDate(Instant start, Instant end, LocalDate date, ZoneId zone) {
        LocalDate startDate = start.atZone(zone).toLocalDate();
        LocalDate endDate = end.atZone(zone).toLocalDate();
        if (startDate.isAfter(date) || endDate.isBefore(date)) {
            return 0;
        }
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        Instant s = start.isAfter(dayStart) ? start : dayStart;
        Instant e = end.isBefore(dayEnd) ? end : dayEnd;
        if (s.isAfter(e)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(s, e);
    }
}
