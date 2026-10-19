package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.SemesterDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 学期 CRUD 接口
 */
@RestController
@RequestMapping("/api/v1/semesters")
public class SemesterController {

    private final SemesterRepository repository;

    public SemesterController(SemesterRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<SemesterDto>> list(Pageable pageable) {
        Page<Semester> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, SemesterDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<SemesterDto> get(@PathVariable Long id) {
        Semester entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学期不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(SemesterDto.from(entity));
    }

    @PostMapping
    public ApiResponse<SemesterDto> create(@Valid @RequestBody Semester semester) {
        return ApiResponse.success("创建成功", SemesterDto.from(repository.save(semester)));
    }

    @PutMapping("/{id}")
    public ApiResponse<SemesterDto> update(@PathVariable Long id, @Valid @RequestBody Semester semester) {
        Semester existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学期不存在: id=" + id));
        checkOwnership(existing);
        existing.setName(semester.getName());
        existing.setStartDate(semester.getStartDate());
        existing.setWeekCount(semester.getWeekCount());
        return ApiResponse.success("更新成功", SemesterDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Semester existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学期不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(Semester entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的学期");
        }
    }
}
