package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.TaskDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/**
 * 任务 CRUD 接口
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskRepository repository;

    public TaskController(TaskRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<TaskDto>> list(Pageable pageable,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            CurrentUserContext.setUserId(userId);
        }
        Page<Task> page;
        if (CurrentUserContext.isLogin()) {
            page = repository.findByUserId(CurrentUserContext.getUserId(), pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return ApiResponse.success(PageDto.from(page, TaskDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskDto> get(@PathVariable Long id) {
        Task entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "任务不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(TaskDto.from(entity));
    }

    @PostMapping
    public ApiResponse<TaskDto> create(@Valid @RequestBody Task task) {
        task.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success("创建成功", TaskDto.from(repository.save(task)));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskDto> update(@PathVariable Long id, @Valid @RequestBody Task task) {
        Task existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "任务不存在: id=" + id));
        checkOwnership(existing);
        existing.setTitle(task.getTitle());
        existing.setDescription(task.getDescription());
        existing.setDeadline(task.getDeadline());
        existing.setRemainingMin(task.getRemainingMin());
        existing.setPriority(task.getPriority());
        existing.setStatus(task.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success("更新成功", TaskDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Task existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "任务不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 数据归属校验：登录状态下，只能操作自己的数据
     */
    private void checkOwnership(Task entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的任务");
        }
    }
}
