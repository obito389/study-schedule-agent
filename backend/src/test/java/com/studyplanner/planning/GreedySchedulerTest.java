package com.studyplanner.planning;

import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.TimeBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 确定性贪心排程测试：排序规则、无硬冲突、缺口报告、确定性。
 */
class GreedySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));

    private GreedyScheduler scheduler;
    private HardConstraintValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HardConstraintValidator();
        scheduler = new GreedyScheduler(validator);
    }

    private TaskSnapshot task(String id, Instant deadline, int minutes, int priority) {
        return new TaskSnapshot(1, id, "任务-" + id, null, deadline, minutes, priority, "PENDING", null);
    }

    private ScheduleSnapshot snapshot(List<TaskSnapshot> tasks) {
        return new ScheduleSnapshot(1, NOW, "Asia/Shanghai", List.of(), tasks, List.of(), List.of(), 1);
    }

    @Test
    void deadlineAscendingOrder() {
        ScheduleSnapshot s = snapshot(List.of(
                task("t1", NOW.plus(Duration.ofDays(2)), 60, 1),
                task("t2", NOW.plus(Duration.ofDays(1)), 60, 1)));
        ScheduleCandidate c = scheduler.schedule(s, CLOCK);
        assertEquals(0, c.gaps().size());
        assertEquals("t2", c.blocks().get(0).id(), "截止更早的任务先排");
        assertEquals("t1", c.blocks().get(1).id());
    }

    @Test
    void priorityDescendingOrder() {
        ScheduleSnapshot s = snapshot(List.of(
                task("t1", NOW.plus(Duration.ofDays(1)), 60, 1),
                task("t2", NOW.plus(Duration.ofDays(1)), 60, 5)));
        ScheduleCandidate c = scheduler.schedule(s, CLOCK);
        assertEquals("t2", c.blocks().get(0).id(), "优先级更高的任务先排");
        assertEquals("t1", c.blocks().get(1).id());
    }

    @Test
    void idAscendingTieBreak() {
        ScheduleSnapshot s = snapshot(List.of(
                task("t2", NOW.plus(Duration.ofDays(1)), 60, 3),
                task("t1", NOW.plus(Duration.ofDays(1)), 60, 3)));
        ScheduleCandidate c = scheduler.schedule(s, CLOCK);
        assertEquals("t1", c.blocks().get(0).id(), "同截止同优先级按 id 升序");
        assertEquals("t2", c.blocks().get(1).id());
    }

    @Test
    void fixedInputProducesConflictFreeSchedule() {
        TimeBlock locked = new TimeBlock(1, "L1", NOW.plus(Duration.ofHours(9)), NOW.plus(Duration.ofHours(11)), true, "上午课程");
        ScheduleSnapshot s = new ScheduleSnapshot(1, NOW, "Asia/Shanghai", List.of(),
                List.of(task("t1", NOW.plus(Duration.ofDays(1)), 120, 3),
                        task("t2", NOW.plus(Duration.ofDays(1)), 90, 2)),
                List.of(locked), List.of(), 1);
        ScheduleCandidate c = scheduler.schedule(s, CLOCK);
        assertEquals(0, c.gaps().size(), "两个任务都应排入");
        for (TimeBlock block : c.blocks()) {
            TaskSnapshot t = s.tasks().stream().filter(x -> x.id().equals(block.id())).findFirst().orElseThrow();
            assertTrue(validator.validate(s, new CandidateBlock(t.id(), block.start(), block.end())).isEmpty(),
                    "排入块 " + block.id() + " 不得违反硬约束");
        }
        assertTrue(c.blocks().stream().noneMatch(b -> b.start().isBefore(NOW)), "排入块不得早于当前时间");
    }

    @Test
    void unplannableTaskReturnsGap() {
        // 截止时间在任务开始之前 → 缺口
        ScheduleSnapshot s = snapshot(List.of(task("t1", NOW.minus(Duration.ofHours(1)), 60, 1)));
        ScheduleCandidate c = scheduler.schedule(s, CLOCK);
        assertEquals(1, c.gaps().size());
        assertEquals("t1", c.gaps().get(0).taskId());
        assertTrue(c.blocks().isEmpty());
    }

    @Test
    void noPartialScheduling() {
        // 截止前只有 30 分钟，任务需要 90 分钟 → 完整排入失败，不得截短
        ScheduleSnapshot s = snapshot(List.of(task("t1", NOW.plus(Duration.ofMinutes(30)), 90, 1)));
        ScheduleCandidate c = scheduler.schedule(s, CLOCK);
        assertEquals(1, c.gaps().size());
        assertEquals("t1", c.gaps().get(0).taskId());
        assertTrue(c.blocks().isEmpty(), "禁止悄悄截短任务");
    }

    @Test
    void alreadyScheduledTaskKept() {
        TimeBlock existing = new TimeBlock(1, "t1", NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(2)), false, "已排");
        TaskSnapshot done = new TaskSnapshot(1, "t1", "任务-t1", null, null, 60, 1, "PENDING", existing);
        ScheduleSnapshot s = snapshot(List.of(done, task("t2", NOW.plus(Duration.ofDays(1)), 60, 1)));
        ScheduleCandidate c = scheduler.schedule(s, CLOCK);
        assertEquals(0, c.gaps().size());
        assertTrue(c.blocks().stream().anyMatch(b -> b.id().equals("t1") && b.start().equals(existing.start())),
                "已排任务的时间块必须保留");
    }

    @Test
    void deterministic100Tasks14Days30Runs() {
        List<TaskSnapshot> tasks = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Instant deadline = NOW.plus(Duration.ofMinutes(30L * i + 15L * (i % 7)));
            tasks.add(task("t" + String.format("%03d", i), deadline, 30 + (i % 4) * 30, 1 + (i % 5)));
        }
        ScheduleSnapshot s = snapshot(tasks);

        long start = System.nanoTime();
        ScheduleCandidate first = scheduler.schedule(s, CLOCK);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        for (int i = 0; i < 29; i++) {
            ScheduleCandidate again = scheduler.schedule(s, CLOCK);
            assertEquals(first.blocks(), again.blocks(), "30 次运行结果必须完全一致");
            assertEquals(first.gaps(), again.gaps());
        }
        assertEquals(100, first.blocks().size() + first.gaps().size());
        assertNotNull(first.blocks());
        assertTrue(elapsedMs < 30_000, "单次排程应在合理时间内完成，实际 " + elapsedMs + " ms");
        System.out.println("[perf] 100 tasks / 14d / single run: " + elapsedMs + " ms");
    }
}
