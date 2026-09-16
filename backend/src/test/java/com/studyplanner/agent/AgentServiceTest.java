package com.studyplanner.agent;

import com.studyplanner.contract.ActionSpec;
import com.studyplanner.contract.ActionStatus;
import com.studyplanner.contract.ActionType;
import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 动作生成测试：创建、移动、取消、信息不足、只读查询和 FAILED 路径。
 * 全程使用 Mock 模型，验证生成的 ActionSpec 结构，不触任何持久化。
 */
class AgentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));

    private AgentService agent;
    private ScheduleSnapshot snapshot;

    @BeforeEach
    void setUp() {
        agent = new AgentService(new MockModelAdapter(), new AgentToolRegistry());
        snapshot = new ScheduleSnapshot(1, NOW, "Asia/Shanghai", List.of(),
                List.of(new TaskSnapshot(1, "t1", "数据结构作业", null,
                        NOW.plus(Duration.ofDays(1)), 60, 1, "PENDING", null)),
                List.of(), List.of(), 7);
    }

    @Test
    void createTaskProducesStructuredActionSpec() {
        AgentResult r = agent.process("创建任务 完成实验报告", snapshot, CLOCK);
        assertEquals(ActionStatus.APPLIED, r.status());
        assertNotNull(r.actionSpec());
        assertEquals(ActionType.CREATE_TASK, r.actionSpec().type());
        assertEquals("完成实验报告", r.actionSpec().changes().get("title"));
        assertEquals(7, r.actionSpec().baseRevision(), "baseRevision 必须来自快照");
        assertNotNull(r.actionSpec().operationKey(), "operationKey 必须非空以支持幂等");
    }

    @Test
    void moveTaskNeedsConfirmation() {
        AgentResult r = agent.process("移动任务 t1 到 明天下午", snapshot, CLOCK);
        assertEquals(ActionStatus.NEEDS_CONFIRMATION, r.status());
        assertNotNull(r.actionSpec());
        assertEquals(ActionType.MOVE_TASK, r.actionSpec().type());
        assertEquals("t1", r.actionSpec().targets().get(0));
        assertEquals("明天下午", r.actionSpec().changes().get("to"));
    }

    @Test
    void cancelTaskNeedsConfirmation() {
        AgentResult r = agent.process("取消任务 t1", snapshot, CLOCK);
        assertEquals(ActionStatus.NEEDS_CONFIRMATION, r.status());
        assertEquals(ActionType.CANCEL_TASK, r.actionSpec().type());
        assertEquals("t1", r.actionSpec().targets().get(0));
    }

    @Test
    void generateScheduleNeedsConfirmation() {
        AgentResult r = agent.process("生成排程", snapshot, CLOCK);
        assertEquals(ActionStatus.NEEDS_CONFIRMATION, r.status());
        assertEquals(ActionType.GENERATE_SCHEDULE, r.actionSpec().type());
    }

    @Test
    void queryScheduleApplied() {
        AgentResult r = agent.process("查询日程", snapshot, CLOCK);
        assertEquals(ActionStatus.APPLIED, r.status());
        assertEquals(ActionType.QUERY_SCHEDULE, r.actionSpec().type());
    }

    @Test
    void insufficientInfoNeedsClarification() {
        AgentResult r = agent.process("创建任务", snapshot, CLOCK);
        assertEquals(ActionStatus.NEEDS_CLARIFICATION, r.status());
        assertNull(r.actionSpec(), "信息不足时不生成动作");
    }

    @Test
    void unknownInstructionNeedsClarification() {
        AgentResult r = agent.process("今天天气怎么样", snapshot, CLOCK);
        assertEquals(ActionStatus.NEEDS_CLARIFICATION, r.status());
        assertNull(r.actionSpec());
    }

    @Test
    void moveMissingTaskNeedsClarification() {
        AgentResult r = agent.process("移动任务 t99 到 明天", snapshot, CLOCK);
        assertEquals(ActionStatus.NEEDS_CLARIFICATION, r.status());
        assertNull(r.actionSpec());
    }

    @Test
    void unregisteredToolFails() {
        // UPDATE_PROFILE 未注册工具 → FAILED，不生成动作
        ModelAdapter adapter = (instruction, s) -> ModelAdapter.Decision.action(ActionType.UPDATE_PROFILE, List.of(), java.util.Map.of());
        AgentService withProfile = new AgentService(adapter, new AgentToolRegistry());
        AgentResult r = withProfile.process("更新偏好", snapshot, CLOCK);
        assertEquals(ActionStatus.FAILED, r.status());
        assertNull(r.actionSpec());
    }

    @Test
    void agentDoesNotTouchPersistence() {
        // AgentService 构造器只接收模型适配与工具注册表，无任何 Repository/端口引用；
        // 返回对象为纯值对象 ActionSpec，验证 Agent 不宣称写入成功。
        AgentResult r = agent.process("创建任务 复习英语", snapshot, CLOCK);
        assertNotNull(r.actionSpec());
        assertTrue(r.actionSpec() instanceof ActionSpec);
        assertNull(r.actionSpec().changes().get("committed"));
    }
}
