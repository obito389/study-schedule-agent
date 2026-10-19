package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.PlanDraftDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/plan-drafts")
public class PlanDraftController {

    private final PlanDraftRepository repository;

    public PlanDraftController(PlanDraftRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<PlanDraftDto>> list(Pageable pageable) {
        Page<PlanDraft> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, PlanDraftDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanDraftDto> get(@PathVariable Long id) {
        PlanDraft entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "计划草稿不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(PlanDraftDto.from(entity));
    }

    @PostMapping
    public ApiResponse<PlanDraftDto> create(@Valid @RequestBody PlanDraft draft) {
        return ApiResponse.success("创建成功", PlanDraftDto.from(repository.save(draft)));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanDraftDto> update(@PathVariable Long id, @Valid @RequestBody PlanDraft draft) {
        PlanDraft existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "计划草稿不存在: id=" + id));
        checkOwnership(existing);
        existing.setStatus(draft.getStatus());
        existing.setProposalJson(draft.getProposalJson());
        existing.setAppliedAt(draft.getAppliedAt());
        return ApiResponse.success("更新成功", PlanDraftDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        PlanDraft existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "计划草稿不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(PlanDraft entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的计划草稿");
        }
    }
}
