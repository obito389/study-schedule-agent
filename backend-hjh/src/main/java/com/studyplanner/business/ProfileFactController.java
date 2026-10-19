package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.ProfileFactDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile-facts")
public class ProfileFactController {

    private final ProfileFactRepository repository;

    public ProfileFactController(ProfileFactRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageDto<ProfileFactDto>> list(Pageable pageable) {
        Page<ProfileFact> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, ProfileFactDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProfileFactDto> get(@PathVariable Long id) {
        ProfileFact entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "画像事实不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(ProfileFactDto.from(entity));
    }

    @PostMapping
    public ApiResponse<ProfileFactDto> create(@Valid @RequestBody ProfileFact fact) {
        return ApiResponse.success("创建成功", ProfileFactDto.from(repository.save(fact)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProfileFactDto> update(@PathVariable Long id, @Valid @RequestBody ProfileFact fact) {
        ProfileFact existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "画像事实不存在: id=" + id));
        checkOwnership(existing);
        existing.setValueJson(fact.getValueJson());
        existing.setState(fact.getState());
        existing.setConfidence(fact.getConfidence());
        existing.setVersion(fact.getVersion());
        return ApiResponse.success("更新成功", ProfileFactDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ProfileFact existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "画像事实不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    private void checkOwnership(ProfileFact entity) {
        if (CurrentUserContext.isLogin()
                && entity.getUserId() != null
                && !entity.getUserId().equals(CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的画像事实");
        }
    }
}
