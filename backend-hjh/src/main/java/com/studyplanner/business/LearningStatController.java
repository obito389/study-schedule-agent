package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.LearningStatDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learning-stats")
public class LearningStatController {

    private final LearningStatRepository repository;

    public LearningStatController(LearningStatRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<LearningStatDto>> list(Pageable pageable) {
        Page<LearningStat> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, LearningStatDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<LearningStatDto> get(@PathVariable Long id) {
        LearningStat entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学习统计不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(LearningStatDto.from(entity));
    }

    @PostMapping
    public ApiResponse<LearningStatDto> create(@Valid @RequestBody LearningStat stat) {
        return ApiResponse.success("创建成功", LearningStatDto.from(repository.save(stat)));
    }

    @PutMapping("/{id}")
    public ApiResponse<LearningStatDto> update(@PathVariable Long id, @Valid @RequestBody LearningStat stat) {
        LearningStat existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学习统计不存在: id=" + id));
        checkOwnership(existing);
        existing.setSampleCount(stat.getSampleCount());
        existing.setRatio(stat.getRatio());
        return ApiResponse.success("更新成功", LearningStatDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        LearningStat existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学习统计不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(LearningStat entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的学习统计");
        }
    }
}
