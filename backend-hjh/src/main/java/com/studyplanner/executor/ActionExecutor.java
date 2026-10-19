package com.studyplanner.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.business.AgentAction;
import com.studyplanner.business.AgentActionRepository;
import com.studyplanner.business.AppUser;
import com.studyplanner.business.AppUserRepository;
import com.studyplanner.business.Task;
import com.studyplanner.business.TaskRepository;
import com.studyplanner.business.TaskStep;
import com.studyplanner.business.TaskStepRepository;
import com.studyplanner.business.TimeBlock;
import com.studyplanner.business.TimeBlockRepository;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.ErrorCode;
import com.studyplanner.enums.ActionStatus;
import com.studyplanner.enums.ActionType;
import com.studyplanner.enums.BlockStatus;
import com.studyplanner.enums.StepStatus;
import com.studyplanner.enums.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动作执行器（Agent 落地的统一事务入口）。
 *
 * <p>核心能力：</p>
 * <ul>
 *   <li>两阶段：{@link #preview} 纯内存模拟不落库，{@link #apply} 单事务批量落地；</li>
 *   <li>乐观锁：apply 用悲观行锁串行化，比对 planning_revision，冲突返回 409，成功后版本 +1；</li>
 *   <li>幂等：以 (user_id, operation_key) 去重，重复提交直接返回首次结果，不重复执行；</li>
 *   <li>硬约束：落地前经 {@link ActionConstraintValidator} 校验，违规整体回滚；</li>
 *   <li>全审计：每个动作写 agent_action，记录 before/after JSON 快照；失败经独立事务记 FAILED。</li>
 * </ul>
 */
@Service
public class ActionExecutor {

    private final AppUserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskStepRepository taskStepRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final AgentActionRepository agentActionRepository;
    private final ActionConstraintValidator constraintValidator;
    private final ObjectMapper objectMapper;

    public ActionExecutor(AppUserRepository userRepository,
                          TaskRepository taskRepository,
                          TaskStepRepository taskStepRepository,
                          TimeBlockRepository timeBlockRepository,
                          AgentActionRepository agentActionRepository,
                          ActionConstraintValidator constraintValidator,
                          ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.agentActionRepository = agentActionRepository;
        this.constraintValidator = constraintValidator;
        this.objectMapper = objectMapper;
    }

    // ==================== 阶段一：预览（不落库） ====================

    @Transactional(readOnly = true)
    public ActionPreviewResult preview(ActionBatchRequest request) {
        Long userId = request.getUserId();
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在: id=" + userId));

        long current = user.getPlanningRevision();
        boolean revisionMatch = current == request.getBaseRevision();
        List<String> violations = new ArrayList<>(constraintValidator.validate(userId, request.getActions()));
        if (!revisionMatch) {
            violations.add(0, "baseRevision 已过期：当前版本=" + current
                    + "，提交基于版本=" + request.getBaseRevision() + "，请重新预览");
        }

        List<ActionItemResult> items = new ArrayList<>();
        if (violations.isEmpty()) {
            for (ActionCommand a : request.getActions()) {
                items.add(simulate(userId, a));
            }
        }

        ActionPreviewResult result = new ActionPreviewResult();
        result.setUserId(userId);
        result.setCurrentRevision(current);
        result.setBaseRevision(request.getBaseRevision());
        result.setRevisionMatch(revisionMatch);
        result.setValid(revisionMatch && violations.isEmpty());
        result.setViolations(violations);
        result.setExpectedRevision(current + 1);
        result.setItems(items);
        return result;
    }

    private ActionItemResult simulate(Long userId, ActionCommand a) {
        return switch (a.getType()) {
            case CREATE_TASK ->
                    ActionItemResult.preview(a, null, "TASK", null, toJson(buildTask(a, userId)));
            case UPDATE_TASK -> {
                Task t = taskRepository.findById(a.getTargetId()).orElse(null);
                if (t == null) {
                    yield ActionItemResult.preview(a, a.getTargetId(), "TASK", null, null);
                }
                t = objectMapper.convertValue(t, Task.class);
                String before = toJson(t);
                mergeTask(t, a);
                yield ActionItemResult.preview(a, t.getId(), "TASK", before, toJson(t));
            }
            case DELETE_TASK -> {
                Task t = taskRepository.findById(a.getTargetId()).orElse(null);
                yield ActionItemResult.preview(a, a.getTargetId(), "TASK", t == null ? null : toJson(t), null);
            }
            case CREATE_STEP ->
                    ActionItemResult.preview(a, null, "STEP", null, toJson(buildStep(a)));
            case UPDATE_STEP -> {
                TaskStep s = taskStepRepository.findById(a.getTargetId()).orElse(null);
                if (s == null) {
                    yield ActionItemResult.preview(a, a.getTargetId(), "STEP", null, null);
                }
                s = objectMapper.convertValue(s, TaskStep.class);
                String before = toJson(s);
                mergeStep(s, a);
                yield ActionItemResult.preview(a, s.getId(), "STEP", before, toJson(s));
            }
            case DELETE_STEP -> {
                TaskStep s = taskStepRepository.findById(a.getTargetId()).orElse(null);
                yield ActionItemResult.preview(a, a.getTargetId(), "STEP", s == null ? null : toJson(s), null);
            }
            case CREATE_BLOCK ->
                    ActionItemResult.preview(a, null, "BLOCK", null,
                            toJson(buildBlock(a, userId, null)));
            case UPDATE_BLOCK -> {
                TimeBlock b = timeBlockRepository.findById(a.getTargetId()).orElse(null);
                if (b == null) {
                    yield ActionItemResult.preview(a, a.getTargetId(), "BLOCK", null, null);
                }
                b = objectMapper.convertValue(b, TimeBlock.class);
                String before = toJson(b);
                mergeBlock(b, a);
                yield ActionItemResult.preview(a, b.getId(), "BLOCK", before, toJson(b));
            }
            case DELETE_BLOCK -> {
                TimeBlock b = timeBlockRepository.findById(a.getTargetId()).orElse(null);
                yield ActionItemResult.preview(a, a.getTargetId(), "BLOCK", b == null ? null : toJson(b), null);
            }
        };
    }

    // ==================== 阶段二：确认（单事务落地） ====================

    @Transactional
    public ActionApplyResult apply(ActionBatchRequest request) {
        Long userId = request.getUserId();

        // 1. 悲观行锁，串行化同一用户的方案落地
        AppUser user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在: id=" + userId));
        long baseRev = user.getPlanningRevision();

        // 2. 幂等检查（优先于乐观锁，保证网络抖动下的重试始终安全）
        List<String> keys = request.getActions().stream()
                .map(ActionCommand::getOperationKey).toList();
        if (keys.stream().distinct().count() != keys.size()) {
            throw new BusinessException(400, "同一批次内 operationKey 不能重复");
        }
        Map<String, AgentAction> existing = agentActionRepository
                .findByUserIdAndOperationKeyIn(userId, keys).stream()
                .collect(Collectors.toMap(AgentAction::getOperationKey, x -> x, (x, y) -> x));

        String requestHash = RequestFingerprint.of(request, objectMapper);
        for (AgentAction prior : existing.values()) {
            if (!requestHash.equals(prior.getRequestHash()) || prior.getStatus() == ActionStatus.UNDONE) {
                throw new BusinessException(ErrorCode.IDEMPOTENT_DUPLICATE,
                        "operationKey 已绑定其他请求、旧记录或已撤销操作，请使用新的操作键");
            }
        }

        long appliedCount = existing.values().stream()
                .filter(x -> x.getStatus() == ActionStatus.APPLIED).count();
        if (appliedCount == request.getActions().size()) {
            return idempotentResult(userId, baseRev, request, existing);
        }
        if (appliedCount > 0) {
            throw new BusinessException(ErrorCode.IDEMPOTENT_DUPLICATE,
                    "本批中存在已应用的操作，为避免重复执行，请整批重试或使用全新的 operationKey");
        }

        // 3. 乐观锁：仅对非重复的新动作校验 planning_revision
        if (baseRev != request.getBaseRevision()) {
            throw new BusinessException(ErrorCode.TASK_VERSION_CONFLICT,
                    "版本冲突：当前版本=" + baseRev + "，提交基于版本=" + request.getBaseRevision()
                            + "，请重新预览后再确认");
        }

        // 4. 硬约束校验（在任何业务写之前）
        // 注意：FAILED 审计不能在当前事务内记录——当前事务持有 app_user 悲观行锁，
        // REQUIRES_NEW 独立事务再写 agent_action（含 user_id 外键）会锁等待超时。
        // 由 ActionExecutorController 在本方法抛出异常、事务回滚释放锁之后再记录。
        List<String> violations = constraintValidator.validate(userId, request.getActions());
        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.ACTION_EXECUTE_FAILED,
                    "硬约束校验失败: " + String.join("; ", violations));
        }

        // 5. 单事务顺序执行全部动作
        long newRev = baseRev + 1;
        Ctx ctx = new Ctx(userId, baseRev, newRev, user.getProfileVersion(),
                toJson(request.getAuthorization()), request.getPlanDraftId(), existing, requestHash);

        List<ActionItemResult> items = new ArrayList<>();
        for (ActionCommand a : request.getActions()) {
            items.add(executeOne(a, request, ctx));
        }

        // 6. 推进 planning_revision
        user.setPlanningRevision(newRev);
        userRepository.save(user);

        ActionApplyResult result = new ActionApplyResult();
        result.setUserId(userId);
        result.setBaseRevision(baseRev);
        result.setNewRevision(newRev);
        result.setIdempotent(false);
        result.setAppliedCount(items.size());
        result.setItems(items);
        return result;
    }

    private ActionApplyResult idempotentResult(Long userId, long rev, ActionBatchRequest request,
                                               Map<String, AgentAction> existing) {
        List<ActionItemResult> items = request.getActions().stream().map(a -> {
            AgentAction e = existing.get(a.getOperationKey());
            return ActionItemResult.idempotent(a, null, entityTypeOf(a.getType()),
                    e.getBeforeJson(), e.getAfterJson());
        }).toList();

        ActionApplyResult result = new ActionApplyResult();
        result.setUserId(userId);
        result.setBaseRevision(rev);
        result.setNewRevision(rev);
        result.setIdempotent(true);
        result.setAppliedCount(0);
        result.setItems(items);
        return result;
    }

    private ActionItemResult executeOne(ActionCommand a, ActionBatchRequest request, Ctx ctx) {
        return switch (a.getType()) {
            case CREATE_TASK -> createTask(a, ctx);
            case UPDATE_TASK -> updateTask(a, ctx);
            case DELETE_TASK -> deleteTask(a, ctx);
            case CREATE_STEP -> createStep(a, ctx);
            case UPDATE_STEP -> updateStep(a, ctx);
            case DELETE_STEP -> deleteStep(a, ctx);
            case CREATE_BLOCK -> createBlock(a, request, ctx);
            case UPDATE_BLOCK -> updateBlock(a, ctx);
            case DELETE_BLOCK -> deleteBlock(a, ctx);
        };
    }

    // ---------------- 任务落地 ----------------

    private ActionItemResult createTask(ActionCommand a, Ctx ctx) {
        Task saved = taskRepository.save(buildTask(a, ctx.userId));
        return record(a, ctx, "TASK", saved.getId(), null, toJson(saved));
    }

    private ActionItemResult updateTask(ActionCommand a, Ctx ctx) {
        Task t = taskRepository.findById(a.getTargetId())
                .orElseThrow(() -> new BusinessException(404, "任务不存在: id=" + a.getTargetId()));
        if (!ctx.userId.equals(t.getUserId())) {
            throw new BusinessException(403, "无权操作他人任务: id=" + t.getId());
        }
        String before = toJson(t);
        mergeTask(t, a);
        t.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(t);
        return record(a, ctx, "TASK", saved.getId(), before, toJson(saved));
    }

    private ActionItemResult deleteTask(ActionCommand a, Ctx ctx) {
        Task t = taskRepository.findById(a.getTargetId())
                .orElseThrow(() -> new BusinessException(404, "任务不存在: id=" + a.getTargetId()));
        if (!ctx.userId.equals(t.getUserId())) {
            throw new BusinessException(403, "无权操作他人任务: id=" + t.getId());
        }
        String before = toJson(t);
        taskRepository.delete(t);
        return record(a, ctx, "TASK", t.getId(), before, null);
    }

    // ---------------- 步骤落地 ----------------

    private ActionItemResult createStep(ActionCommand a, Ctx ctx) {
        TaskStep saved = taskStepRepository.save(buildStep(a));
        return record(a, ctx, "STEP", saved.getId(), null, toJson(saved));
    }

    private ActionItemResult updateStep(ActionCommand a, Ctx ctx) {
        TaskStep s = taskStepRepository.findById(a.getTargetId())
                .orElseThrow(() -> new BusinessException(404, "任务步骤不存在: id=" + a.getTargetId()));
        requireStepOwned(ctx.userId, s);
        String before = toJson(s);
        mergeStep(s, a);
        TaskStep saved = taskStepRepository.save(s);
        return record(a, ctx, "STEP", saved.getId(), before, toJson(saved));
    }

    private ActionItemResult deleteStep(ActionCommand a, Ctx ctx) {
        TaskStep s = taskStepRepository.findById(a.getTargetId())
                .orElseThrow(() -> new BusinessException(404, "任务步骤不存在: id=" + a.getTargetId()));
        requireStepOwned(ctx.userId, s);
        String before = toJson(s);
        taskStepRepository.delete(s);
        return record(a, ctx, "STEP", s.getId(), before, null);
    }

    // ---------------- 时间块落地 ----------------

    private ActionItemResult createBlock(ActionCommand a, ActionBatchRequest request, Ctx ctx) {
        TimeBlock saved = timeBlockRepository.save(buildBlock(a, ctx.userId, request.getPlanDraftId()));
        return record(a, ctx, "BLOCK", saved.getId(), null, toJson(saved));
    }

    private ActionItemResult updateBlock(ActionCommand a, Ctx ctx) {
        TimeBlock b = timeBlockRepository.findById(a.getTargetId())
                .orElseThrow(() -> new BusinessException(404, "时间块不存在: id=" + a.getTargetId()));
        if (!ctx.userId.equals(b.getUserId())) {
            throw new BusinessException(403, "无权操作他人时间块: id=" + b.getId());
        }
        String before = toJson(b);
        mergeBlock(b, a);
        TimeBlock saved = timeBlockRepository.save(b);
        return record(a, ctx, "BLOCK", saved.getId(), before, toJson(saved));
    }

    private ActionItemResult deleteBlock(ActionCommand a, Ctx ctx) {
        TimeBlock b = timeBlockRepository.findById(a.getTargetId())
                .orElseThrow(() -> new BusinessException(404, "时间块不存在: id=" + a.getTargetId()));
        if (!ctx.userId.equals(b.getUserId())) {
            throw new BusinessException(403, "无权操作他人时间块: id=" + b.getId());
        }
        String before = toJson(b);
        timeBlockRepository.delete(b);
        return record(a, ctx, "BLOCK", b.getId(), before, null);
    }

    // ---------------- 构造 / 合并（preview 与 apply 共用） ----------------

    private Task buildTask(ActionCommand a, Long userId) {
        Task t = new Task();
        t.setUserId(userId);
        t.setTitle(a.getTitle());
        t.setDescription(a.getDescription());
        t.setDeadline(a.getDeadline());
        t.setRemainingMin(a.getRemainingMin() == null ? 0 : a.getRemainingMin());
        t.setPriority(a.getPriority() == null ? 2 : a.getPriority());
        t.setStatus(a.getStatus() == null ? TaskStatus.TODO : a.getStatus());
        t.setTaskType(a.getTaskType());
        t.setCourseId(a.getCourseId());
        t.setInitialEstimateMin(a.getInitialEstimateMin());
        return t;
    }

    private void mergeTask(Task t, ActionCommand a) {
        if (a.getTitle() != null) t.setTitle(a.getTitle());
        if (a.getDescription() != null) t.setDescription(a.getDescription());
        if (a.getDeadline() != null) t.setDeadline(a.getDeadline());
        if (a.getRemainingMin() != null) t.setRemainingMin(a.getRemainingMin());
        if (a.getPriority() != null) t.setPriority(a.getPriority());
        if (a.getStatus() != null) t.setStatus(a.getStatus());
        if (a.getTaskType() != null) t.setTaskType(a.getTaskType());
        if (a.getCourseId() != null) t.setCourseId(a.getCourseId());
        if (a.getInitialEstimateMin() != null) t.setInitialEstimateMin(a.getInitialEstimateMin());
    }

    private TaskStep buildStep(ActionCommand a) {
        TaskStep s = new TaskStep();
        s.setTaskId(a.getTaskId());
        s.setOrderNo(a.getOrderNo());
        s.setTitle(a.getStepTitle());
        s.setRemainingMin(a.getStepRemainingMin() == null ? 0 : a.getStepRemainingMin());
        s.setStatus(a.getStepStatus() == null ? StepStatus.TODO : a.getStepStatus());
        return s;
    }

    private void mergeStep(TaskStep s, ActionCommand a) {
        if (a.getOrderNo() != null) s.setOrderNo(a.getOrderNo());
        if (a.getStepTitle() != null) s.setTitle(a.getStepTitle());
        if (a.getStepRemainingMin() != null) s.setRemainingMin(a.getStepRemainingMin());
        if (a.getStepStatus() != null) s.setStatus(a.getStepStatus());
    }

    private TimeBlock buildBlock(ActionCommand a, Long userId, Long batchDraftId) {
        TimeBlock b = new TimeBlock();
        b.setUserId(userId);
        b.setTaskId(a.getTaskId());
        b.setStepId(a.getStepId());
        b.setSourceDraftId(a.getSourceDraftId() != null ? a.getSourceDraftId() : batchDraftId);
        b.setStartAt(a.getStartAt());
        b.setEndAt(a.getEndAt());
        b.setStatus(a.getBlockStatus() == null ? BlockStatus.PLANNED : a.getBlockStatus());
        b.setLocked(a.getLocked() == null ? false : a.getLocked());
        return b;
    }

    private void mergeBlock(TimeBlock b, ActionCommand a) {
        if (a.getStepId() != null) b.setStepId(a.getStepId());
        if (a.getSourceDraftId() != null) b.setSourceDraftId(a.getSourceDraftId());
        if (a.getStartAt() != null) b.setStartAt(a.getStartAt());
        if (a.getEndAt() != null) b.setEndAt(a.getEndAt());
        if (a.getBlockStatus() != null) b.setStatus(a.getBlockStatus());
        if (a.getLocked() != null) b.setLocked(a.getLocked());
    }

    // ---------------- 审计与工具 ----------------

    private ActionItemResult record(ActionCommand a, Ctx ctx, String entityType,
                                    Long targetId, String before, String after) {
        AgentAction action = ctx.existing.get(a.getOperationKey());
        if (action == null) {
            action = new AgentAction();
            action.setUserId(ctx.userId);
            action.setOperationKey(a.getOperationKey());
            action.setType(a.getType());
        }
        action.setRequestHash(ctx.requestHash);
        action.setStatus(ActionStatus.APPLIED);
        action.setBaseRevision(ctx.baseRev);
        action.setAppliedRevision(ctx.newRev);
        action.setPlanDraftId(ctx.planDraftId);
        action.setBaseProfileVersion(ctx.profileVer);
        action.setAuthorizationJson(ctx.authJson);
        action.setBeforeJson(before);
        action.setAfterJson(after);
        action.setErrorCode(null);
        agentActionRepository.save(action);
        return ActionItemResult.applied(a, targetId, entityType, before, after);
    }

    private void requireStepOwned(Long userId, TaskStep s) {
        boolean owned = taskRepository.findById(s.getTaskId())
                .map(t -> userId.equals(t.getUserId()))
                .orElse(false);
        if (!owned) {
            throw new BusinessException(403, "无权操作他人任务步骤: id=" + s.getId());
        }
    }

    private String entityTypeOf(ActionType type) {
        return switch (type) {
            case CREATE_TASK, UPDATE_TASK, DELETE_TASK -> "TASK";
            case CREATE_STEP, UPDATE_STEP, DELETE_STEP -> "STEP";
            case CREATE_BLOCK, UPDATE_BLOCK, DELETE_BLOCK -> "BLOCK";
        };
    }

    private String toJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "执行快照序列化失败: " + e.getMessage());
        }
    }

    /** 一批动作共享的执行上下文 */
    private static final class Ctx {
        final Long userId;
        final long baseRev;
        final long newRev;
        final long profileVer;
        final String authJson;
        /** 来源草案ID，写入 agent_action.plan_draft_id，供第 4 周批量撤销分组 */
        final Long planDraftId;
        final Map<String, AgentAction> existing;
        final String requestHash;

        Ctx(Long userId, long baseRev, long newRev, long profileVer, String authJson, Long planDraftId,
            Map<String, AgentAction> existing, String requestHash) {
            this.userId = userId;
            this.baseRev = baseRev;
            this.newRev = newRev;
            this.profileVer = profileVer;
            this.authJson = authJson;
            this.planDraftId = planDraftId;
            this.existing = existing;
            this.requestHash = requestHash;
        }
    }
}
