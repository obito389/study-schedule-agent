package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.AgentActionDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent-actions")
public class AgentActionController {

    private final AgentActionRepository repository;

    public AgentActionController(AgentActionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<AgentActionDto>> list(Pageable pageable) {
        Page<AgentAction> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, AgentActionDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentActionDto> get(@PathVariable Long id) {
        AgentAction entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Agent动作不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(AgentActionDto.from(entity));
    }

    @PostMapping
    public ApiResponse<AgentActionDto> create(@Valid @RequestBody AgentAction action) {
        return ApiResponse.success("创建成功", AgentActionDto.from(repository.save(action)));
    }

    @PutMapping("/{id}")
    public ApiResponse<AgentActionDto> update(@PathVariable Long id, @Valid @RequestBody AgentAction action) {
        AgentAction existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Agent动作不存在: id=" + id));
        checkOwnership(existing);
        existing.setStatus(action.getStatus());
        existing.setAppliedRevision(action.getAppliedRevision());
        existing.setAfterJson(action.getAfterJson());
        existing.setErrorCode(action.getErrorCode());
        return ApiResponse.success("更新成功", AgentActionDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AgentAction existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Agent动作不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(AgentAction entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的动作记录");
        }
    }
}
