package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.UserPreferenceDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-preferences")
public class UserPreferenceController {

    private final UserPreferenceRepository repository;

    public UserPreferenceController(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<UserPreferenceDto>> list(Pageable pageable) {
        Page<UserPreference> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, UserPreferenceDto::from));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserPreferenceDto> get(@PathVariable Long userId) {
        UserPreference entity = repository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户偏好不存在: userId=" + userId));
        checkOwnership(userId);
        return ApiResponse.success(UserPreferenceDto.from(entity));
    }

    @PostMapping
    public ApiResponse<UserPreferenceDto> create(@Valid @RequestBody UserPreference preference) {
        return ApiResponse.success("创建成功", UserPreferenceDto.from(repository.save(preference)));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserPreferenceDto> update(@PathVariable Long userId, @Valid @RequestBody UserPreference preference) {
        UserPreference existing = repository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户偏好不存在: userId=" + userId));
        checkOwnership(userId);
        existing.setDailyLimitMin(preference.getDailyLimitMin());
        existing.setBlockMin(preference.getBlockMin());
        existing.setHorizonDays(preference.getHorizonDays());
        return ApiResponse.success("更新成功", UserPreferenceDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable Long userId) {
        UserPreference existing = repository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户偏好不存在: userId=" + userId));
        checkOwnership(userId);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 用户偏好主键即 userId，直接比对路径参数与当前登录用户
     */
    private void checkOwnership(Long userId) {
        if (CurrentUserContext.isLogin()
                && !userId.equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的偏好设置");
        }
    }
}
