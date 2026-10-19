package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.dto.AppUserDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/**
 * 账户 CRUD 接口
 */
@RestController
@RequestMapping("/api/v1/users")
public class AppUserController {

    private final AppUserRepository repository;

    public AppUserController(AppUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<AppUserDto>> list(Pageable pageable) {
        return ApiResponse.success(PageDto.from(repository.findAll(pageable), AppUserDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppUserDto> get(@PathVariable Long id) {
        AppUser user = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在: id=" + id));
        return ApiResponse.success(AppUserDto.from(user));
    }

    @PostMapping
    public ApiResponse<AppUserDto> create(@Valid @RequestBody AppUser user) {
        // id、版本号、创建时间由系统管理，忽略客户端传入，防止污染乐观锁版本
        user.setId(null);
        user.setPlanningRevision(0L);
        user.setProfileVersion(0L);
        user.setCreatedAt(LocalDateTime.now());
        AppUser saved = repository.save(user);
        return ApiResponse.success("创建成功", AppUserDto.from(saved));
    }

    @PutMapping("/{id}")
    public ApiResponse<AppUserDto> update(@PathVariable Long id, @Valid @RequestBody AppUser user) {
        AppUser existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在: id=" + id));
        existing.setUsername(user.getUsername());
        existing.setTimezone(user.getTimezone());
        existing.setLearningEnabled(user.getLearningEnabled());
        return ApiResponse.success("更新成功", AppUserDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new BusinessException(404, "用户不存在: id=" + id);
        }
        repository.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }
}
