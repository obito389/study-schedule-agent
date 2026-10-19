package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.ExecutionLogDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/execution-logs")
public class ExecutionLogController {

    private final ExecutionLogRepository repository;

    public ExecutionLogController(ExecutionLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<ExecutionLogDto>> list(Pageable pageable) {
        Page<ExecutionLog> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, ExecutionLogDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExecutionLogDto> get(@PathVariable Long id) {
        ExecutionLog entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "执行日志不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(ExecutionLogDto.from(entity));
    }

    @PostMapping
    public ApiResponse<ExecutionLogDto> create(@Valid @RequestBody ExecutionLog log) {
        return ApiResponse.success("创建成功", ExecutionLogDto.from(repository.save(log)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ExecutionLog existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "执行日志不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(ExecutionLog entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的执行日志");
        }
    }
}
