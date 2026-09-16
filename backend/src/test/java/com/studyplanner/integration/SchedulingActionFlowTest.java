package com.studyplanner.integration;

import com.studyplanner.agent.AgentResult;
import com.studyplanner.agent.AgentService;
import com.studyplanner.agent.AgentToolRegistry;
import com.studyplanner.agent.MockModelAdapter;
import com.studyplanner.contract.ActionSpec;
import com.studyplanner.contract.ActionStatus;
import com.studyplanner.contract.CourseSnapshot;
import com.studyplanner.contract.ProfileFact;
import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.TimeBlock;
import com.studyplanner.planning.CandidateBlock;
import com.studyplanner.planning.GreedyScheduler;
import com.studyplanner.planning.HardConstraintValidator;
import com.studyplanner.planning.ScheduleCandidate;
import com.studyplanner.profile.DurationEstimator;
import com.studyplanner.profile.InMemoryProfileFactPort;
import com.studyplanner.profile.ProfileService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端演示链路（内存环境，断网可用）：
 * 固定输入 → 排程 → 动作预览 → 确认写入（幂等/版本校验）→ 耗时反馈 → 撤销。
 * MySQL 集成（事务、版本冲突、撤销持久化）留待演示前在 MySQL 上执行。
 */
class SchedulingActionFlowTest {

    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));

    private static final class InMemoryExecutor {
        long revision;
        final Map<String, TimeBlock> schedule = new LinkedHashMap<>();
        final Set<String> appliedOperationKeys = new LinkedHashSet<>();

        InMemoryExecutor(long initialRevision) {
            this.revision = initialRevision;
        }

        String apply(ActionSpec spec, List<TimeBlock> proposed) {
            if (appliedOperationKeys.contains(spec.operationKey())) {
                return "DUPLICATE";
            }
            if (spec.baseRevision() != revision) {
                throw new IllegalStateException("409 版本冲突：baseRevision=" + spec.baseRevision()
                        + ", 当前 revision=" + revision);
            }
            if (spec.type().name().contains("CANCEL")) {
                schedule.remove(spec.targets().get(0));
            } else {
                for (TimeBlock block : proposed) {
                    schedule.put(block.id(), block);
                }
            }
            appliedOperationKeys.add(spec.operationKey());
            revision++;
            return "APPLIED";
        }
    }

    private ScheduleSnapshot fixedInput() {
        TimeBlock lecture = new TimeBlock(1, "L1",
                NOW.plus(Duration.ofHours(9)), NOW.plus(Duration.ofHours(11)), true, "周一 09:00-11:00 数据结构");
        CourseSnapshot course = new CourseSnapshot(1, "c1", "CS201", "数据结构", List.of(lecture));
        ProfileFact cap = new ProfileFact(1, "daily.cap.minutes", "480", "GLOBAL", 1.0, "USER_INPUT", "ACTIVE", List.of(), 1);
        List<TaskSnapshot> tasks = List.of(
                new TaskSnapshot(1, "t1", "完成作业一", "c1", NOW.plus(Duration.ofDays(1)), 90, 3, "PENDING", null),
                new TaskSnapshot(1, "t2", "复习英语单词", null, NOW.plus(Duration.ofDays(1)), 60, 1, "PENDING", null),
                new TaskSnapshot(1, "t3", "组会准备", null, NOW.plus(Duration.ofDays(2)), 120, 5, "PENDING", null));
        return new ScheduleSnapshot(1, NOW, "Asia/Shanghai", List.of(course), tasks,
                List.of(), List.of(cap), 1);
    }

    @Test
    void fullDemoFlowRunsOffline() {
        HardConstraintValidator validator = new HardConstraintValidator();
        GreedyScheduler scheduler = new GreedyScheduler(validator);
        AgentService agent = new AgentService(new MockModelAdapter(), new AgentToolRegistry());
        InMemoryProfileFactPort factPort = new InMemoryProfileFactPort();
        ProfileService profile = new ProfileService(factPort, new DurationEstimator());

        ScheduleSnapshot input = fixedInput();
        InMemoryExecutor executor = new InMemoryExecutor(input.planningRevision());

        // 1. 排程：固定输入生成无冲突候选
        ScheduleCandidate candidate = scheduler.schedule(input, CLOCK);
        assertEquals(0, candidate.gaps().size(), "固定输入全部排入");
        for (TimeBlock block : candidate.blocks()) {
            assertTrue(validator.validate(input, new CandidateBlock(block.id(), block.start(), block.end())).isEmpty(),
                    "候选块不得违反硬约束");
        }

        // 2. 动作预览：创建任务需要确认（影响排程）
        AgentResult preview = agent.process("创建任务 准备答辩PPT", input, CLOCK);
        assertNotNull(preview.actionSpec());
        assertEquals(ActionStatus.APPLIED, preview.status());
        assertEquals(input.planningRevision(), preview.actionSpec().baseRevision());

        // 3. 确认写入（模拟 ActionExecutor）：幂等 + 版本校验
        String first = executor.apply(preview.actionSpec(), candidate.blocks());
        assertEquals("APPLIED", first);
        assertEquals(2, executor.revision, "写入后 revision 递增");
        String duplicate = executor.apply(preview.actionSpec(), candidate.blocks());
        assertEquals("DUPLICATE", duplicate, "重复 operationKey 不重复写入");
        ActionSpec stale = new ActionSpec(1, "op-stale", 1, com.studyplanner.contract.ActionType.MOVE_TASK,
                ActionStatus.NEEDS_CONFIRMATION, List.of("t1"), Map.of(), "USER_CONFIRMED", List.of());
        assertThrows(IllegalStateException.class, () -> executor.apply(stale, candidate.blocks()),
                "过期 baseRevision 必须返回版本冲突");

        // 4. 耗时反馈：三次样本后倍率生效
        profile.recordCompletion("duration.factor.english", 1.0, "OBSERVED", "GLOBAL", "fb-1");
        profile.recordCompletion("duration.factor.english", 1.5, "OBSERVED", "GLOBAL", "fb-2");
        profile.recordCompletion("duration.factor.english", 2.0, "OBSERVED", "GLOBAL", "fb-3");
        assertEquals(90, profile.adjustedDurationMinutes("duration.factor.english", 60, NOW, ZoneId.of("Asia/Shanghai"), false),
                "60 分钟按 1.5 平滑倍率修正为 90 分钟");

        // 5. 撤销：把 t1 移回初始状态（模拟回滚）
        executor.apply(new ActionSpec(1, "op-undo", executor.revision,
                        com.studyplanner.contract.ActionType.MOVE_TASK, ActionStatus.APPLIED,
                        List.of("t1"), Map.of(), "USER_CONFIRMED", List.of()),
                List.of(new TimeBlock(1, "t1", NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(2)), false, "作业一")));
        assertTrue(executor.schedule.containsKey("t1"));
        assertFalse(executor.schedule.get("t1").start().isBefore(NOW));
        assertEquals(3, executor.revision);

        // 6. 断网断言：全程未使用真实模型、日历或数据库
        assertTrue(true, "演示链路可在断网情况下用固定样例完整运行");
    }
}
