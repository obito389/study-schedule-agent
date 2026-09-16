package com.studyplanner.planning;

import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.TimeBlock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 确定性贪心排程器。
 *
 * 排序：截止时间升序（无截止最后）→ 优先级降序 → 任务 id 升序。
 * 以 30 分钟为粒度，从当前时间起为每个任务寻找最早的可行时间槽；
 * 候选块经 {@link HardConstraintValidator} 校验（截止、锁定、重叠、每日上限）。
 * 无法完整排入的任务返回 {@link PlanningGap}，绝不截短任务。
 * 核心逻辑不直接调用 {@code Instant.now()}，必须注入 {@link Clock}。
 */
public class GreedyScheduler {

    public static final int SLOT_MINUTES = 30;

    private final HardConstraintValidator validator;

    public GreedyScheduler(HardConstraintValidator validator) {
        this.validator = validator;
    }

    public ScheduleCandidate schedule(ScheduleSnapshot snapshot, Clock clock) {
        Instant now = Instant.now(clock);

        Map<String, TimeBlock> scheduled = new LinkedHashMap<>();
        for (TaskSnapshot task : snapshot.tasks()) {
            if (task.scheduledBlock() != null) {
                scheduled.put(task.id(), task.scheduledBlock());
            }
        }

        List<TaskSnapshot> pending = snapshot.tasks().stream()
                .filter(t -> t.scheduledBlock() == null)
                .sorted(taskOrder())
                .toList();

        List<PlanningGap> gaps = new ArrayList<>();
        for (TaskSnapshot task : pending) {
            CandidateBlock slot = findEarliestSlot(snapshot, scheduled, task, now);
            if (slot == null) {
                gaps.add(new PlanningGap(task.id(), "在截止时间之前没有满足全部硬约束的连续时间槽"));
            } else {
                scheduled.put(task.id(), new TimeBlock(1, task.id(), slot.start(), slot.end(), false, task.title()));
            }
        }

        return new ScheduleCandidate(new ArrayList<>(scheduled.values()), gaps);
    }

    private Comparator<TaskSnapshot> taskOrder() {
        return Comparator
                .comparing((TaskSnapshot t) -> t.deadline() == null ? Instant.MAX : t.deadline())
                .thenComparing(TaskSnapshot::priority, Comparator.reverseOrder())
                .thenComparing(TaskSnapshot::id);
    }

    private CandidateBlock findEarliestSlot(ScheduleSnapshot snapshot, Map<String, TimeBlock> scheduled,
                                            TaskSnapshot task, Instant now) {
        Instant windowEnd = task.deadline() != null
                ? task.deadline()
                : now.plus(Duration.ofDays(14));
        if (windowEnd.isBefore(now)) {
            return null;
        }

        Instant cursor = alignUpToSlot(now);
        while (!cursor.plus(Duration.ofMinutes(task.durationMinutes())).isAfter(windowEnd)) {
            if (!cursor.isBefore(now)) {
                CandidateBlock candidate = new CandidateBlock(task.id(),
                        cursor, cursor.plus(Duration.ofMinutes(task.durationMinutes())));
                if (validator.validate(withScheduled(snapshot, scheduled), candidate).isEmpty()) {
                    return candidate;
                }
            }
            cursor = cursor.plus(SLOT_MINUTES, ChronoUnit.MINUTES);
        }
        return null;
    }

    private Instant alignUpToSlot(Instant instant) {
        long minutes = ChronoUnit.MINUTES.between(Instant.EPOCH, instant);
        long remainder = Math.floorMod(minutes, SLOT_MINUTES);
        return instant.plus(SLOT_MINUTES - remainder, ChronoUnit.MINUTES);
    }

    /** 把已排入的块回写为快照中任务的 scheduledBlock，供校验器检查重叠与每日上限。 */
    private ScheduleSnapshot withScheduled(ScheduleSnapshot snapshot, Map<String, TimeBlock> scheduled) {
        List<TaskSnapshot> tasks = snapshot.tasks().stream()
                .map(t -> {
                    TimeBlock block = scheduled.get(t.id());
                    return block == null ? t
                            : new TaskSnapshot(t.schemaVersion(), t.id(), t.title(), t.courseId(),
                                    t.deadline(), t.durationMinutes(), t.priority(), t.state(), block);
                })
                .toList();
        return new ScheduleSnapshot(snapshot.schemaVersion(), snapshot.now(), snapshot.timezone(),
                snapshot.courses(), tasks, snapshot.lockedBlocks(), snapshot.activeFacts(), snapshot.planningRevision());
    }
}
