package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.TimeBlockDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 时间块 CRUD 接口
 */
@RestController
@RequestMapping("/api/v1/time-blocks")
public class TimeBlockController {

    private final TimeBlockRepository repository;
    private final TaskRepository taskRepository;

    public TimeBlockController(TimeBlockRepository repository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public ApiResponse<PageDto<TimeBlockDto>> list(Pageable pageable) {
        Page<TimeBlock> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, TimeBlockDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<TimeBlockDto> get(@PathVariable Long id) {
        TimeBlock entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "时间块不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(TimeBlockDto.from(entity));
    }

    @PostMapping
    public ApiResponse<TimeBlockDto> create(@Valid @RequestBody TimeBlock block) {
        // 登录用户只能在自己任务下创建时间块，且归属用户以登录身份为准（防伪造 userId）
        if (CurrentUserContext.isLogin()) {
            if (block.getTaskId() != null) {
                Task task = taskRepository.findById(block.getTaskId())
                        .orElseThrow(() -> new BusinessException(404, "关联任务不存在: id=" + block.getTaskId()));
                if (!task.getUserId().equals(CurrentUserContext.getUserId())) {
                    throw new BusinessException(403, "无权在他人任务下创建时间块");
                }
            }
            block.setUserId(CurrentUserContext.getUserId());
        }
        return ApiResponse.success("创建成功", TimeBlockDto.from(repository.save(block)));
    }

    @PutMapping("/{id}")
    public ApiResponse<TimeBlockDto> update(@PathVariable Long id, @Valid @RequestBody TimeBlock block) {
        TimeBlock existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "时间块不存在: id=" + id));
        checkOwnership(existing);
        existing.setStartAt(block.getStartAt());
        existing.setEndAt(block.getEndAt());
        existing.setStatus(block.getStatus());
        existing.setLocked(block.getLocked());
        return ApiResponse.success("更新成功", TimeBlockDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        TimeBlock existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "时间块不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(TimeBlock entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的时间块");
        }
    }
}
