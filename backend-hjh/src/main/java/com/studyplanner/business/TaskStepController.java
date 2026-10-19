package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.TaskStepDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 任务步骤 CRUD 接口
 * task_step 通过 task_id → task.user_id 间接归属用户
 */
@RestController
@RequestMapping("/api/v1/task-steps")
public class TaskStepController {

    private final TaskStepRepository repository;
    private final TaskRepository taskRepository;

    public TaskStepController(TaskStepRepository repository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public ApiResponse<PageDto<TaskStepDto>> list(Pageable pageable) {
        Page<TaskStep> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, TaskStepDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskStepDto> get(@PathVariable Long id) {
        TaskStep entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "任务步骤不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(TaskStepDto.from(entity));
    }

    @PostMapping
    public ApiResponse<TaskStepDto> create(@Valid @RequestBody TaskStep step) {
        checkTaskOwnership(step.getTaskId());
        return ApiResponse.success("创建成功", TaskStepDto.from(repository.save(step)));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskStepDto> update(@PathVariable Long id, @Valid @RequestBody TaskStep step) {
        TaskStep existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "任务步骤不存在: id=" + id));
        checkOwnership(existing);
        checkTaskOwnership(step.getTaskId());
        existing.setOrderNo(step.getOrderNo());
        existing.setTitle(step.getTitle());
        existing.setRemainingMin(step.getRemainingMin());
        existing.setStatus(step.getStatus());
        return ApiResponse.success("更新成功", TaskStepDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        TaskStep existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "任务步骤不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    /** 单条数据归属校验：步骤所属任务必须属于当前用户 */
    private void checkOwnership(TaskStep entity) {
        if (CurrentUserContext.isLogin()
                && !repository.existsByIdAndUserId(entity.getId(), CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的任务步骤");
        }
    }

    /** 创建校验：目标任务必须属于当前用户 */
    private void checkTaskOwnership(Long taskId) {
        if (!CurrentUserContext.isLogin()) {
            return;
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "关联任务不存在: id=" + taskId));
        if (!CurrentUserContext.getUserId().equals(task.getUserId())) {
            throw new BusinessException(403, "无权在他人任务下创建步骤");
        }
    }
}
