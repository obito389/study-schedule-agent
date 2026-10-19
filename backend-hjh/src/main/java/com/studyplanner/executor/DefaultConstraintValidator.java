package com.studyplanner.executor;

import com.studyplanner.business.CourseRepository;
import com.studyplanner.business.Task;
import com.studyplanner.business.TaskRepository;
import com.studyplanner.business.TaskStep;
import com.studyplanner.business.TaskStepRepository;
import com.studyplanner.business.TimeBlock;
import com.studyplanner.business.TimeBlockRepository;
import com.studyplanner.enums.BlockStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 默认硬约束校验实现。
 * 覆盖：字段合法性、外键存在性与归属、时间块与已有安排冲突（以及同批内部冲突）。
 * 熊翔的 ActionSpec 交付后可整体替换本类。
 */
@Component
public class DefaultConstraintValidator implements ActionConstraintValidator {

    private final TaskRepository taskRepository;
    private final TaskStepRepository taskStepRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final CourseRepository courseRepository;

    public DefaultConstraintValidator(TaskRepository taskRepository,
                                      TaskStepRepository taskStepRepository,
                                      TimeBlockRepository timeBlockRepository,
                                      CourseRepository courseRepository) {
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public List<String> validate(Long userId, List<ActionCommand> actions) {
        List<String> violations = new ArrayList<>();
        List<BlockSpan> batchBlocks = new ArrayList<>();

        for (int i = 0; i < actions.size(); i++) {
            ActionCommand a = actions.get(i);
            String tag = "第" + (i + 1) + "个动作[" + a.getOperationKey() + "/" + a.getType() + "] ";
            switch (a.getType()) {
                case CREATE_TASK -> validateCreateTask(userId, a, tag, violations);
                case UPDATE_TASK -> validateUpdateTask(userId, a, tag, violations);
                case DELETE_TASK -> validateDeleteTask(userId, a, tag, violations);
                case CREATE_STEP -> validateCreateStep(userId, a, tag, violations);
                case UPDATE_STEP -> validateUpdateStep(userId, a, tag, violations);
                case DELETE_STEP -> validateDeleteStep(userId, a, tag, violations);
                case CREATE_BLOCK -> validateCreateBlock(userId, a, tag, violations, batchBlocks);
                case UPDATE_BLOCK -> validateUpdateBlock(userId, a, tag, violations, batchBlocks);
                case DELETE_BLOCK -> validateDeleteBlock(userId, a, tag, violations);
            }
        }

        // 同批动作内部时间块重叠检测
        for (int i = 0; i < batchBlocks.size(); i++) {
            for (int j = i + 1; j < batchBlocks.size(); j++) {
                if (batchBlocks.get(i).overlaps(batchBlocks.get(j))) {
                    violations.add("同批动作中的时间块相互重叠: "
                            + batchBlocks.get(i).label + " 与 " + batchBlocks.get(j).label);
                }
            }
        }
        return violations;
    }

    // ---------------- 任务 ----------------

    private void validateCreateTask(Long userId, ActionCommand a, String tag, List<String> v) {
        if (isBlank(a.getTitle())) {
            v.add(tag + "任务标题不能为空");
        }
        if (a.getRemainingMin() != null && a.getRemainingMin() < 0) {
            v.add(tag + "任务剩余时间不能为负");
        }
        if (a.getPriority() != null && (a.getPriority() < 1 || a.getPriority() > 3)) {
            v.add(tag + "任务优先级必须在 1~3 之间");
        }
        if (a.getCourseId() != null && !courseRepository.existsByIdAndUserId(a.getCourseId(), userId)) {
            v.add(tag + "关联课程不存在或不属于当前用户: courseId=" + a.getCourseId());
        }
    }

    private void validateUpdateTask(Long userId, ActionCommand a, String tag, List<String> v) {
        requireOwnedTask(userId, a.getTargetId(), tag, v);
        if (a.getRemainingMin() != null && a.getRemainingMin() < 0) {
            v.add(tag + "任务剩余时间不能为负");
        }
        if (a.getPriority() != null && (a.getPriority() < 1 || a.getPriority() > 3)) {
            v.add(tag + "任务优先级必须在 1~3 之间");
        }
        if (a.getCourseId() != null && !courseRepository.existsByIdAndUserId(a.getCourseId(), userId)) {
            v.add(tag + "关联课程不存在或不属于当前用户: courseId=" + a.getCourseId());
        }
    }

    private void validateDeleteTask(Long userId, ActionCommand a, String tag, List<String> v) {
        requireOwnedTask(userId, a.getTargetId(), tag, v);
    }

    // ---------------- 步骤 ----------------

    private void validateCreateStep(Long userId, ActionCommand a, String tag, List<String> v) {
        requireOwnedTask(userId, a.getTaskId(), tag, v);
        if (a.getOrderNo() == null || a.getOrderNo() < 1) {
            v.add(tag + "步骤排序号(orderNo)必须≥1");
        }
        if (isBlank(a.getStepTitle())) {
            v.add(tag + "步骤标题(stepTitle)不能为空");
        }
        if (a.getStepRemainingMin() != null && a.getStepRemainingMin() < 0) {
            v.add(tag + "步骤剩余时间不能为负");
        }
    }

    private void validateUpdateStep(Long userId, ActionCommand a, String tag, List<String> v) {
        requireOwnedStep(userId, a.getTargetId(), tag, v);
        if (a.getTaskId() != null) {
            requireOwnedTask(userId, a.getTaskId(), tag, v);
        }
        if (a.getOrderNo() != null && a.getOrderNo() < 1) {
            v.add(tag + "步骤排序号(orderNo)必须≥1");
        }
        if (a.getStepRemainingMin() != null && a.getStepRemainingMin() < 0) {
            v.add(tag + "步骤剩余时间不能为负");
        }
    }

    private void validateDeleteStep(Long userId, ActionCommand a, String tag, List<String> v) {
        requireOwnedStep(userId, a.getTargetId(), tag, v);
    }

    // ---------------- 时间块 ----------------

    private void validateCreateBlock(Long userId, ActionCommand a, String tag, List<String> v,
                                     List<BlockSpan> batchBlocks) {
        requireOwnedTask(userId, a.getTaskId(), tag, v);
        checkStepBelongsToTask(userId, a, tag, v);
        if (checkTimeRange(a, tag, v)) {
            checkOverlap(userId, a, tag, v, batchBlocks, null);
        }
    }

    private void validateUpdateBlock(Long userId, ActionCommand a, String tag, List<String> v,
                                     List<BlockSpan> batchBlocks) {
        TimeBlock self = requireOwnedBlock(userId, a.getTargetId(), tag, v);
        if (self == null) return;
        if (Boolean.TRUE.equals(self.getLocked())) {
            v.add(tag + "时间块已锁定，不可修改");
            return;
        }
        checkStepBelongsToTask(userId, a, tag, v);
        LocalDateTime s = a.getStartAt();
        LocalDateTime e = a.getEndAt();
        // 更新支持部分更新：开始/结束都不传视为不改时间（如仅标记完成），无需做重叠检测
        if (s == null && e == null) {
            return;
        }
        if (s == null || e == null) {
            v.add(tag + "开始时间与结束时间必须成对提供");
            return;
        }
        if (!e.isAfter(s)) {
            v.add(tag + "时间块结束时间必须晚于开始时间: " + s + " ~ " + e);
            return;
        }
        checkOverlap(userId, a, tag, v, batchBlocks, self == null ? null : self.getId());
    }

    private void validateDeleteBlock(Long userId, ActionCommand a, String tag, List<String> v) {
        TimeBlock self = requireOwnedBlock(userId, a.getTargetId(), tag, v);
        if (self != null && Boolean.TRUE.equals(self.getLocked())) {
            v.add(tag + "时间块已锁定，不可删除");
        }
    }

    // ---------------- 归属与取值辅助 ----------------

    private Task requireOwnedTask(Long userId, Long taskId, String tag, List<String> v) {
        if (taskId == null) {
            v.add(tag + "缺少任务ID(taskId/targetId)");
            return null;
        }
        Optional<Task> opt = taskRepository.findById(taskId);
        if (opt.isEmpty()) {
            v.add(tag + "任务不存在: id=" + taskId);
            return null;
        }
        if (!userId.equals(opt.get().getUserId())) {
            v.add(tag + "无权操作他人任务: id=" + taskId);
            return null;
        }
        return opt.get();
    }

    private void requireOwnedStep(Long userId, Long stepId, String tag, List<String> v) {
        if (stepId == null) {
            v.add(tag + "缺少步骤ID(targetId)");
            return;
        }
        Optional<TaskStep> opt = taskStepRepository.findById(stepId);
        if (opt.isEmpty()) {
            v.add(tag + "任务步骤不存在: id=" + stepId);
            return;
        }
        TaskStep step = opt.get();
        boolean owned = taskRepository.findById(step.getTaskId())
                .map(t -> userId.equals(t.getUserId()))
                .orElse(false);
        if (!owned) {
            v.add(tag + "无权操作他人任务步骤: id=" + stepId);
        }
    }

    private TimeBlock requireOwnedBlock(Long userId, Long blockId, String tag, List<String> v) {
        if (blockId == null) {
            v.add(tag + "缺少时间块ID(targetId)");
            return null;
        }
        Optional<TimeBlock> opt = timeBlockRepository.findById(blockId);
        if (opt.isEmpty()) {
            v.add(tag + "时间块不存在: id=" + blockId);
            return null;
        }
        if (!userId.equals(opt.get().getUserId())) {
            v.add(tag + "无权操作他人时间块: id=" + blockId);
            return null;
        }
        return opt.get();
    }

    private void checkStepBelongsToTask(Long userId, ActionCommand a, String tag, List<String> v) {
        if (a.getStepId() == null) {
            return;
        }
        Optional<TaskStep> opt = taskStepRepository.findById(a.getStepId());
        if (opt.isEmpty()) {
            v.add(tag + "关联步骤不存在: stepId=" + a.getStepId());
            return;
        }
        TaskStep step = opt.get();
        if (a.getTaskId() != null && !a.getTaskId().equals(step.getTaskId())) {
            v.add(tag + "步骤不属于所给任务: stepId=" + a.getStepId());
            return;
        }
        boolean owned = taskRepository.findById(step.getTaskId())
                .map(t -> userId.equals(t.getUserId()))
                .orElse(false);
        if (!owned) {
            v.add(tag + "无权在他人任务的步骤上安排时间块: stepId=" + a.getStepId());
        }
    }

    /** 校验时间区间非空且 end > start，返回是否可继续做重叠检测 */
    private boolean checkTimeRange(ActionCommand a, String tag, List<String> v) {
        LocalDateTime s = a.getStartAt();
        LocalDateTime e = a.getEndAt();
        if (s == null || e == null) {
            v.add(tag + "时间块必须提供开始时间(startAt)和结束时间(endAt)");
            return false;
        }
        if (!e.isAfter(s)) {
            v.add(tag + "时间块结束时间必须晚于开始时间: " + s + " ~ " + e);
            return false;
        }
        return true;
    }

    private void checkOverlap(Long userId, ActionCommand a, String tag, List<String> v,
                              List<BlockSpan> batchBlocks, Long selfId) {
        LocalDateTime s = a.getStartAt();
        LocalDateTime e = a.getEndAt();
        for (TimeBlock b : timeBlockRepository.findOverlapping(userId, s, e)) {
            if (b.getStatus() == BlockStatus.CANCELLED) {
                continue;
            }
            if (selfId != null && selfId.equals(b.getId())) {
                continue;
            }
            v.add(tag + "时间块与已有安排冲突: blockId=" + b.getId()
                    + " (" + b.getStartAt() + " ~ " + b.getEndAt() + ")");
            break;
        }
        BlockSpan span = new BlockSpan();
        span.label = tag;
        span.start = s;
        span.end = e;
        batchBlocks.add(span);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 同批内时间块区间，用于检测批次内部冲突 */
    private static class BlockSpan {
        String label;
        LocalDateTime start;
        LocalDateTime end;

        boolean overlaps(BlockSpan o) {
            return start.isBefore(o.end) && end.isAfter(o.start);
        }
    }
}
