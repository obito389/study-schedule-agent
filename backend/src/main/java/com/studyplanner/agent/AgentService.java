package com.studyplanner.agent;

import com.studyplanner.contract.ActionSpec;
import com.studyplanner.contract.ActionStatus;
import com.studyplanner.contract.ActionType;
import com.studyplanner.contract.ScheduleSnapshot;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Agent 服务。读取 ScheduleSnapshot，经模型适配生成结构化 ActionSpec。
 *
 * 约束：一次请求最多进行一次规划与一次工具选择；Agent 只生成 ActionSpec，
 * 不直接访问 Repository、不拼接 SQL、不宣称写入成功。影响其他任务或多对象的
 * 动作返回 NEEDS_CONFIRMATION，由 ActionExecutor 在短事务内校验 baseRevision
 * 与硬约束后提交。
 */
public class AgentService {

    private final ModelAdapter modelAdapter;
    private final AgentToolRegistry toolRegistry;

    public AgentService(ModelAdapter modelAdapter, AgentToolRegistry toolRegistry) {
        this.modelAdapter = modelAdapter;
        this.toolRegistry = toolRegistry;
    }

    public AgentResult process(String instruction, ScheduleSnapshot snapshot, Clock clock) {
        ModelAdapter.Decision decision = modelAdapter.decide(instruction, snapshot);
        if (decision.clarificationMessage() != null) {
            return new AgentResult(ActionStatus.NEEDS_CLARIFICATION, null, decision.clarificationMessage());
        }
        if (!toolRegistry.toolKeys().contains(toolKeyFor(decision.type()))) {
            return new AgentResult(ActionStatus.FAILED, null, "工具未注册: " + decision.type());
        }

        ActionStatus status = statusFor(decision.type());
        ActionSpec spec = new ActionSpec(
                1,
                "op-" + UUID.randomUUID(),
                snapshot.planningRevision(),
                decision.type(),
                status,
                decision.targets(),
                decision.changes(),
                "USER_CONFIRMED",
                List.of());

        String message = switch (status) {
            case NEEDS_CONFIRMATION -> "动作影响其他任务或整体排程，已生成预览，等待确认";
            default -> "动作已生成";
        };
        return new AgentResult(status, spec, message);
    }

    private ActionStatus statusFor(ActionType type) {
        return switch (type) {
            case QUERY_SCHEDULE, CREATE_TASK -> ActionStatus.APPLIED;
            case GENERATE_SCHEDULE, MOVE_TASK, CANCEL_TASK, UPDATE_PROFILE -> ActionStatus.NEEDS_CONFIRMATION;
        };
    }

    private String toolKeyFor(ActionType type) {
        return switch (type) {
            case QUERY_SCHEDULE -> "query_schedule";
            case GENERATE_SCHEDULE -> "generate_schedule";
            case CREATE_TASK -> "create_task";
            case MOVE_TASK -> "move_task";
            case CANCEL_TASK -> "cancel_task";
            case UPDATE_PROFILE -> null;
        };
    }
}
