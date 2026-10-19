package com.studyplanner.undo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.studyplanner.enums.BlockStatus;
import com.studyplanner.enums.StepStatus;
import com.studyplanner.enums.TaskStatus;
import com.studyplanner.executor.ActionItemResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 撤销执行器（第 4 周）：把已 APPLIED 的动作按快照反向回滚。
 *
 * <p>核心设计：</p>
 * <ul>
 *   <li><b>反向映射</b>：CREATE_* 撤销 = 删除新行；UPDATE_* 撤销 = 用 before_json 回写旧值；
 *       DELETE_* 撤销 = 用 before_json 重建行（以新主键）。</li>
 *   <li><b>逆序批量</b>：批量撤销必须按 id 倒序执行。原 apply 是「先建父后建子」「先删子后删父」，
 *       倒序撤销正好是「先删子后删父」「先建父后建子」，不会出现子记录挂在已被删除的父上。</li>
 *   <li><b>版本推进</b>：撤销本身是一次写操作，沿用悲观行锁 + 乐观锁，成功后 planning_revision +1。</li>
 *   <li><b>全审计</b>：原动作标记为 UNDONE；同时新增一条 APPLIED 记录（operationKey 带 undo-{rev}- 前缀，
 *       记录撤销前后的快照），保证链路可追溯且不撞 (user_id, operation_key) 唯一约束。</li>
 * </ul>
 */
@Service
public class UndoExecutor {

    private final AppUserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskStepRepository taskStepRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final AgentActionRepository agentActionRepository;
    private final ObjectMapper objectMapper;

    public UndoExecutor(AppUserRepository userRepository,
                        TaskRepository taskRepository,
                        TaskStepRepository taskStepRepository,
                        TimeBlockRepository timeBlockRepository,
                        AgentActionRepository agentActionRepository,
                        ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.agentActionRepository = agentActionRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== 单步撤销 ====================

    @Transactional
    public UndoResult undoOne(UndoRequest request) {
        String key = request.getOperationKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException(400, "单步撤销必须提供 operationKey");
        }
        AppUser user = lockUser(request.getUserId());
        AgentAction action = agentActionRepository
                .findByUserIdAndOperationKey(request.getUserId(), key)
                .orElseThrow(() -> new BusinessException(404, "动作记录不存在: operationKey=" + key));
        return doUndo(request, List.of(action), user);
    }

    // ==================== 批量撤销（按草案整体回滚） ====================

    @Transactional
    public UndoResult undoBatch(UndoRequest request) {
        Long userId = request.getUserId();
        Long draftId = request.getPlanDraftId();
        if (draftId == null) {
            throw new BusinessException(400, "批量撤销必须提供 planDraftId");
        }
        AppUser user = lockUser(userId);
        List<AgentAction> all = agentActionRepository.findByUserIdAndPlanDraftId(userId, draftId);
        if (all.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_ACTION_TO_UNDO,
                    "该草案没有可撤销的动作: planDraftId=" + draftId
                            + "（提示：存量记录未写入 plan_draft_id，批量撤销仅对新增记录生效）");
        }
        boolean hasUndone = all.stream().anyMatch(x -> x.getStatus() == ActionStatus.UNDONE);
        if (hasUndone) {
            throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED,
                    "该草案已部分撤销，不可重复整体撤销: planDraftId=" + draftId);
        }
        // FAILED 动作从未落地，无需撤销；只回滚 APPLIED，且按 id 倒序（逆序撤销）
        List<AgentAction> applied = all.stream()
                .filter(x -> x.getStatus() == ActionStatus.APPLIED)
                .sorted(Comparator.comparing(AgentAction::getId).reversed())
                .toList();
        if (applied.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_ACTION_TO_UNDO,
                    "该草案没有可撤销的动作: planDraftId=" + draftId);
        }
        return doUndo(request, applied, user);
    }

    // ==================== 历史版本查询 ====================

    @Transactional(readOnly = true)
    public List<UndoHistoryItem> history(Long userId, Long planDraftId) {
        List<AgentAction> actions = planDraftId != null
                ? agentActionRepository.findByUserIdAndPlanDraftId(userId, planDraftId)
                : agentActionRepository.findByUserId(userId);
        return actions.stream()
                .sorted(Comparator.comparing(AgentAction::getId))
                .map(UndoHistoryItem::from)
                .toList();
    }

    // ==================== 主流程：加锁 → 校验版本 → 逐个反向 → 推进版本 ====================

    private AppUser lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在: id=" + userId));
    }

    private UndoResult doUndo(UndoRequest request, List<AgentAction> actions, AppUser user) {
        Long userId = request.getUserId();

        // User lock was acquired before loading the action records.

        // 2. 乐观锁：撤销基于的版本必须与当前一致
        long baseRev = user.getPlanningRevision();
        if (baseRev != request.getBaseRevision()) {
            throw new BusinessException(ErrorCode.TASK_VERSION_CONFLICT,
                    "版本冲突：当前版本=" + baseRev + "，提交基于版本=" + request.getBaseRevision()
                            + "，请刷新后重试");
        }

        // 3. 单事务逐个反向回滚（任一失败整批回滚）
        long newRev = baseRev + 1;
        List<ActionItemResult> items = new ArrayList<>();
        for (AgentAction a : actions) {
            items.add(undoOneAction(a, user, baseRev, newRev));
        }

        // 4. 推进 planning_revision
        user.setPlanningRevision(newRev);
        userRepository.save(user);

        UndoResult result = new UndoResult();
        result.setUserId(userId);
        result.setBaseRevision(baseRev);
        result.setNewRevision(newRev);
        result.setUndoneCount(items.size());
        result.setItems(items);
        return result;
    }

    /** 反向回滚单个动作，并写入「原动作 UNDONE + 撤销记录 APPLIED」两条审计 */
    private ActionItemResult undoOneAction(AgentAction a, AppUser user, long baseRev, long newRev) {
        if (a.getStatus() != ActionStatus.APPLIED) {
            throw new BusinessException(ErrorCode.ACTION_NOT_UNDOABLE,
                    "该动作当前状态不可撤销: operationKey=" + a.getOperationKey() + "，status=" + a.getStatus());
        }
        if (!user.getId().equals(a.getUserId())) {
            throw new BusinessException(403, "无权撤销他人动作: operationKey=" + a.getOperationKey());
        }

        String beforeSnapshot = null;
        String afterSnapshot = null;
        Long targetId = null;
        String entityType = null;

        switch (a.getType()) {
            case CREATE_TASK -> {
                Long id = snapshotId(a.getAfterJson(), a.getOperationKey());
                Task t = taskRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND,
                                "撤销失败：任务已不存在: id=" + id));
                requireTaskUndoable(t);
                requireUnchanged(t, a);
                beforeSnapshot = toJson(t);
                targetId = t.getId();
                entityType = "TASK";
                taskRepository.delete(t);
            }
            case UPDATE_TASK -> {
                Long id = snapshotId(a.getBeforeJson(), a.getOperationKey());
                Task cur = taskRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND,
                                "撤销失败：任务已不存在: id=" + id));
                requireTaskUndoable(cur);
                requireUnchanged(cur, a);
                Task old = fromJson(a.getBeforeJson(), Task.class);
                beforeSnapshot = toJson(cur);
                restoreTask(cur, old);
                taskRepository.save(cur);
                targetId = cur.getId();
                entityType = "TASK";
                afterSnapshot = toJson(cur);
            }
            case DELETE_TASK -> {
                Task old = fromJson(a.getBeforeJson(), Task.class);
                old.setId(null);
                Task saved = taskRepository.save(old);
                targetId = saved.getId();
                entityType = "TASK";
                afterSnapshot = toJson(saved);
            }
            case CREATE_STEP -> {
                Long id = snapshotId(a.getAfterJson(), a.getOperationKey());
                TaskStep s = taskStepRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_STEP_NOT_FOUND,
                                "撤销失败：任务步骤已不存在: id=" + id));
                requireStepUndoable(s);
                requireUnchanged(s, a);
                beforeSnapshot = toJson(s);
                targetId = s.getId();
                entityType = "STEP";
                taskStepRepository.delete(s);
            }
            case UPDATE_STEP -> {
                Long id = snapshotId(a.getBeforeJson(), a.getOperationKey());
                TaskStep cur = taskStepRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_STEP_NOT_FOUND,
                                "撤销失败：任务步骤已不存在: id=" + id));
                requireStepUndoable(cur);
                requireUnchanged(cur, a);
                TaskStep old = fromJson(a.getBeforeJson(), TaskStep.class);
                beforeSnapshot = toJson(cur);
                cur.setTaskId(old.getTaskId());
                cur.setOrderNo(old.getOrderNo());
                cur.setTitle(old.getTitle());
                cur.setRemainingMin(old.getRemainingMin());
                cur.setStatus(old.getStatus());
                taskStepRepository.save(cur);
                targetId = cur.getId();
                entityType = "STEP";
                afterSnapshot = toJson(cur);
            }
            case DELETE_STEP -> {
                TaskStep old = fromJson(a.getBeforeJson(), TaskStep.class);
                old.setId(null);
                TaskStep saved = taskStepRepository.save(old);
                targetId = saved.getId();
                entityType = "STEP";
                afterSnapshot = toJson(saved);
            }
            case CREATE_BLOCK -> {
                Long id = snapshotId(a.getAfterJson(), a.getOperationKey());
                TimeBlock b = timeBlockRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TIME_BLOCK_NOT_FOUND,
                                "撤销失败：时间块已不存在: id=" + id));
                requireBlockUndoable(b);
                requireUnchanged(b, a);
                beforeSnapshot = toJson(b);
                targetId = b.getId();
                entityType = "BLOCK";
                timeBlockRepository.delete(b);
            }
            case UPDATE_BLOCK -> {
                Long id = snapshotId(a.getBeforeJson(), a.getOperationKey());
                TimeBlock cur = timeBlockRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TIME_BLOCK_NOT_FOUND,
                                "撤销失败：时间块已不存在: id=" + id));
                requireBlockUndoable(cur);
                requireUnchanged(cur, a);
                TimeBlock old = fromJson(a.getBeforeJson(), TimeBlock.class);
                beforeSnapshot = toJson(cur);
                restoreBlock(cur, old);
                timeBlockRepository.save(cur);
                targetId = cur.getId();
                entityType = "BLOCK";
                afterSnapshot = toJson(cur);
            }
            case DELETE_BLOCK -> {
                TimeBlock old = fromJson(a.getBeforeJson(), TimeBlock.class);
                old.setId(null);
                TimeBlock saved = timeBlockRepository.save(old);
                targetId = saved.getId();
                entityType = "BLOCK";
                afterSnapshot = toJson(saved);
            }
        }

        // 原动作标记为已撤销（保留记录，便于追溯完整链路）
        a.setStatus(ActionStatus.UNDONE);
        agentActionRepository.save(a);

        // 新增一条撤销审计记录（operationKey 带 undo-{rev}- 前缀，避免撞唯一约束）
        AgentAction undoRecord = new AgentAction();
        undoRecord.setUserId(a.getUserId());
        undoRecord.setOperationKey(undoOperationKey(a.getOperationKey(), newRev));
        undoRecord.setType(a.getType());
        undoRecord.setStatus(ActionStatus.APPLIED);
        undoRecord.setBaseRevision(baseRev);
        undoRecord.setAppliedRevision(newRev);
        undoRecord.setPlanDraftId(a.getPlanDraftId());
        undoRecord.setBaseProfileVersion(user.getProfileVersion());
        undoRecord.setBeforeJson(beforeSnapshot);
        undoRecord.setAfterJson(afterSnapshot);
        agentActionRepository.save(undoRecord);

        return ActionItemResult.undone(a.getOperationKey(), a.getType(), targetId, entityType,
                beforeSnapshot, afterSnapshot);
    }

    // ==================== 字段回写 ====================

    private void restoreTask(Task cur, Task old) {
        cur.setTitle(old.getTitle());
        cur.setDescription(old.getDescription());
        cur.setDeadline(old.getDeadline());
        cur.setRemainingMin(old.getRemainingMin());
        cur.setPriority(old.getPriority());
        cur.setStatus(old.getStatus());
        cur.setTaskType(old.getTaskType());
        cur.setCourseId(old.getCourseId());
        cur.setImportJobId(old.getImportJobId());
        cur.setInitialEstimateMin(old.getInitialEstimateMin());
        cur.setEstimateSource(old.getEstimateSource());
    }

    private void restoreBlock(TimeBlock cur, TimeBlock old) {
        cur.setTaskId(old.getTaskId());
        cur.setStepId(old.getStepId());
        cur.setSourceDraftId(old.getSourceDraftId());
        cur.setStartAt(old.getStartAt());
        cur.setEndAt(old.getEndAt());
        cur.setStatus(old.getStatus());
        cur.setLocked(old.getLocked());
    }

    // ==================== 边界校验（已完成 / 已锁定不可撤销） ====================

    private void requireTaskUndoable(Task t) {
        if (t.getStatus() == TaskStatus.DONE) {
            throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED, "任务已完成，不可撤销: id=" + t.getId());
        }
    }

    private void requireStepUndoable(TaskStep s) {
        if (s.getStatus() == StepStatus.DONE) {
            throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED, "任务步骤已完成，不可撤销: id=" + s.getId());
        }
    }

    private void requireBlockUndoable(TimeBlock b) {
        if (b.getStatus() == BlockStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED, "时间块已完成，不可撤销: id=" + b.getId());
        }
        if (Boolean.TRUE.equals(b.getLocked())) {
            throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED, "时间块已锁定，不可撤销: id=" + b.getId());
        }
    }

    // ==================== 快照读写 ====================

    /** Ignore only persistence timestamps; all business fields must still match. */
    private void requireUnchanged(Object current, AgentAction action) {
        if (action.getAfterJson() == null) {
            throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED, "缺少执行后快照，不可撤销");
        }
        try {
            var expected = objectMapper.readTree(action.getAfterJson());
            var actual = objectMapper.readTree(toJson(current));
            if (!(expected instanceof com.fasterxml.jackson.databind.node.ObjectNode expectedObject)
                    || !(actual instanceof com.fasterxml.jackson.databind.node.ObjectNode actualObject)) {
                throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED, "动作快照格式错误");
            }
            expectedObject.remove(List.of("createdAt", "updatedAt"));
            actualObject.remove(List.of("createdAt", "updatedAt"));
            if (!expectedObject.equals(actualObject)) {
                throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED,
                        "对象已被后续操作修改，请刷新后重新选择撤销范围");
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.UNDO_NOT_ALLOWED, "动作快照无法解析，不可撤销");
        }
    }

    /** 从 before_json / after_json 中取出实体主键（快照即 JPA 实体序列化结果，含 id 字段） */
    private Long snapshotId(String json, String operationKey) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(400, "动作快照缺失，无法撤销: operationKey=" + operationKey);
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode idNode = node.get("id");
            if (idNode == null || idNode.isNull()) {
                throw new BusinessException(400, "动作快照缺少主键 id，无法撤销: operationKey=" + operationKey);
            }
            return idNode.asLong();
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "动作快照解析失败: " + e.getMessage());
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(400, "动作快照缺失，无法撤销");
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "动作快照反序列化失败: " + e.getMessage());
        }
    }

    private String toJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "撤销快照序列化失败: " + e.getMessage());
        }
    }

    /**
     * 撤销记录的 operationKey：加「undo-{newRev}-」前缀。
     * newRev 每次撤销单调递增且同一批内唯一，因此天然避免与历史 key（含重复撤销同一 key）撞唯一约束；
     * 超长时截断到 64 字符，唯一前缀优先保留。
     */
    private String undoOperationKey(String originalKey, long newRev) {
        String k = "undo-" + newRev + "-" + originalKey;
        return k.length() > 64 ? k.substring(0, 64) : k;
    }
}
