package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.AvailabilityWindowDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/availability-windows")
public class AvailabilityWindowController {

    private final AvailabilityWindowRepository repository;

    public AvailabilityWindowController(AvailabilityWindowRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<AvailabilityWindowDto>> list(Pageable pageable) {
        Page<AvailabilityWindow> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, AvailabilityWindowDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<AvailabilityWindowDto> get(@PathVariable Long id) {
        AvailabilityWindow entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "可用时间窗口不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(AvailabilityWindowDto.from(entity));
    }

    @PostMapping
    public ApiResponse<AvailabilityWindowDto> create(@Valid @RequestBody AvailabilityWindow window) {
        return ApiResponse.success("创建成功", AvailabilityWindowDto.from(repository.save(window)));
    }

    @PutMapping("/{id}")
    public ApiResponse<AvailabilityWindowDto> update(@PathVariable Long id, @Valid @RequestBody AvailabilityWindow window) {
        AvailabilityWindow existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "可用时间窗口不存在: id=" + id));
        checkOwnership(existing);
        existing.setKind(window.getKind());
        existing.setWeekday(window.getWeekday());
        existing.setLocalDate(window.getLocalDate());
        existing.setStartTime(window.getStartTime());
        existing.setEndTime(window.getEndTime());
        return ApiResponse.success("更新成功", AvailabilityWindowDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AvailabilityWindow existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "可用时间窗口不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(AvailabilityWindow entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的时间窗口");
        }
    }
}
