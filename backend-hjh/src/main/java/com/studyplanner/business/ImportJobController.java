package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.ImportJobDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/import-jobs")
public class ImportJobController {

    private final ImportJobRepository repository;

    public ImportJobController(ImportJobRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<ImportJobDto>> list(Pageable pageable) {
        Page<ImportJob> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, ImportJobDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<ImportJobDto> get(@PathVariable Long id) {
        ImportJob entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "导入任务不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(ImportJobDto.from(entity));
    }

    @PostMapping
    public ApiResponse<ImportJobDto> create(@Valid @RequestBody ImportJob job) {
        return ApiResponse.success("创建成功", ImportJobDto.from(repository.save(job)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ImportJobDto> update(@PathVariable Long id, @Valid @RequestBody ImportJob job) {
        ImportJob existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "导入任务不存在: id=" + id));
        checkOwnership(existing);
        existing.setState(job.getState());
        existing.setProposalJson(job.getProposalJson());
        existing.setErrorCode(job.getErrorCode());
        return ApiResponse.success("更新成功", ImportJobDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ImportJob existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "导入任务不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(ImportJob entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的导入任务");
        }
    }
}
