package com.studyplanner.planning;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

import com.studyplanner.contract.ProfileFact;
import com.studyplanner.contract.ProfileFactState;
import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.TimeBlock;
import com.studyplanner.contract.TimeBlockKind;

public final class HardConstraintValidator {

    public static final String DAILY_LIMIT_FACT_KEY = "planning.dailyLimitMinutes";

    private static final Comparator<ConstraintViolation> VIOLATION_ORDER = Comparator
            .comparingInt((ConstraintViolation violation) -> violation.code().sortOrder())
            .thenComparing(violation -> String.join("\u0000", violation.objectIds()));

    public List<ConstraintViolation> validate(ScheduleSnapshot snapshot, CandidateBlock candidate) {
        Objects.requireNonNull(snapshot, "snapshot is required");
        Objects.requireNonNull(candidate, "candidate is required");

        List<ConstraintViolation> violations = new ArrayList<>();
        TaskSnapshot task = snapshot.tasks().stream()
                .filter(item -> item.id().equals(candidate.taskId()))
                .findFirst()
                .orElse(null);
        if (task == null) {
            violations.add(new ConstraintViolation(
                    ConstraintCode.TASK_NOT_FOUND,
                    List.of(candidate.taskId()),
                    "候选时间块引用的任务不存在"));
            return List.copyOf(violations);
        }

        if (candidate.end().isAfter(task.deadline())) {
            violations.add(new ConstraintViolation(
                    ConstraintCode.DEADLINE_EXCEEDED,
                    List.of(task.id()),
                    "候选时间块结束时间晚于任务截止时间"));
        }

        List<TimeBlock> existingBlocks = existingBlocks(snapshot);
        for (TimeBlock existing : existingBlocks) {
            if (!overlaps(candidate.start(), candidate.end(), existing.start(), existing.end())) {
                continue;
            }
            violations.add(overlapViolation(task.id(), existing));
        }

        dailyLimit(snapshot).ifPresent(limit -> addDailyLimitViolations(
                violations,
                snapshot,
                candidate,
                existingBlocks,
                limit));

        violations.sort(VIOLATION_ORDER);
        return List.copyOf(violations);
    }

    /** Course and fixed-block writes share the same overlap rules as task scheduling. */
    public List<ConstraintViolation> validateFixedBlock(ScheduleSnapshot snapshot, TimeBlock candidate) {
        return existingBlocks(snapshot).stream()
                .filter(existing -> overlaps(candidate.start(), candidate.end(), existing.start(), existing.end()))
                .map(existing -> overlapViolation(candidate.ownerId(), existing))
                .sorted(VIOLATION_ORDER).toList();
    }

    private static List<TimeBlock> existingBlocks(ScheduleSnapshot snapshot) {
        Map<String, TimeBlock> unique = new LinkedHashMap<>();
        snapshot.courses().forEach(course -> course.meetingBlocks()
                .forEach(block -> unique.putIfAbsent(block.id(), block)));
        snapshot.tasks().forEach(task -> task.scheduledBlocks()
                .forEach(block -> unique.putIfAbsent(block.id(), block)));
        snapshot.lockedBlocks().forEach(block -> unique.putIfAbsent(block.id(), block));
        return unique.values().stream()
                .sorted(Comparator.comparing(TimeBlock::id))
                .toList();
    }

    private static ConstraintViolation overlapViolation(String taskId, TimeBlock existing) {
        if (existing.kind() == TimeBlockKind.UNAVAILABLE) {
            return new ConstraintViolation(
                    ConstraintCode.FORBIDDEN_PERIOD,
                    List.of(taskId, existing.id()),
                    "候选时间块落入禁止安排的时段");
        }
        if (existing.locked()) {
            return new ConstraintViolation(
                    ConstraintCode.LOCKED_BLOCK_OVERLAP,
                    List.of(taskId, existing.id()),
                    "候选时间块与锁定时间块重叠");
        }
        return new ConstraintViolation(
                ConstraintCode.TIME_OVERLAP,
                List.of(taskId, existing.id()),
                "候选时间块与已有安排重叠");
    }

    private static boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private static OptionalInt dailyLimit(ScheduleSnapshot snapshot) {
        return snapshot.activeFacts().stream()
                .filter(fact -> fact.state() == ProfileFactState.ACTIVE)
                .filter(fact -> DAILY_LIMIT_FACT_KEY.equals(fact.key()))
                .filter(fact -> fact.value().canConvertToInt())
                .filter(fact -> fact.value().intValue() > 0)
                .max(Comparator.comparingLong(ProfileFact::version))
                .map(fact -> OptionalInt.of(fact.value().intValue()))
                .orElseGet(OptionalInt::empty);
    }

    private static void addDailyLimitViolations(
            List<ConstraintViolation> violations,
            ScheduleSnapshot snapshot,
            CandidateBlock candidate,
            List<TimeBlock> existingBlocks,
            int dailyLimitMinutes) {
        ZoneId zone = ZoneId.of(snapshot.timezone());
        LocalDate firstDate = candidate.start().atZone(zone).toLocalDate();
        LocalDate lastDate = candidate.end().minusNanos(1).atZone(zone).toLocalDate();

        for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            long candidateMinutes = minutesWithinDate(candidate.start(), candidate.end(), currentDate, zone);
            long existingMinutes = existingBlocks.stream()
                    .filter(block -> block.kind() == TimeBlockKind.TASK)
                    .mapToLong(block -> minutesWithinDate(block.start(), block.end(), currentDate, zone))
                    .sum();
            if (candidateMinutes + existingMinutes > dailyLimitMinutes) {
                violations.add(new ConstraintViolation(
                        ConstraintCode.DAILY_LIMIT_EXCEEDED,
                        List.of(candidate.taskId(), currentDate.toString()),
                        "候选时间块使 " + currentDate + " 的学习时长超过每日上限 " + dailyLimitMinutes + " 分钟"));
            }
        }
    }

    private static long minutesWithinDate(Instant start, Instant end, LocalDate date, ZoneId zone) {
        Instant dateStart = date.atStartOfDay(zone).toInstant();
        Instant dateEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        Instant overlapStart = start.isAfter(dateStart) ? start : dateStart;
        Instant overlapEnd = end.isBefore(dateEnd) ? end : dateEnd;
        if (!overlapStart.isBefore(overlapEnd)) {
            return 0;
        }
        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }
}
