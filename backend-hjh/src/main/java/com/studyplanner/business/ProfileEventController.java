package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.ProfileEventDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile-events")
public class ProfileEventController {

    private final ProfileEventRepository repository;

    public ProfileEventController(ProfileEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<ProfileEventDto>> list(Pageable pageable) {
        Page<ProfileEvent> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, ProfileEventDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProfileEventDto> get(@PathVariable Long id) {
        ProfileEvent entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "画像事件不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(ProfileEventDto.from(entity));
    }

    @PostMapping
    public ApiResponse<ProfileEventDto> create(@Valid @RequestBody ProfileEvent event) {
        return ApiResponse.success("创建成功", ProfileEventDto.from(repository.save(event)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ProfileEvent existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "画像事件不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(ProfileEvent entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的画像事件");
        }
    }
}
