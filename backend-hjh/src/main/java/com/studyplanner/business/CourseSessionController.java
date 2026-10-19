package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.CourseSessionDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 课程节次 CRUD 接口
 * course_session 通过 course_id → course → semester.user_id 间接归属用户
 */
@RestController
@RequestMapping("/api/v1/course-sessions")
public class CourseSessionController {

    private final CourseSessionRepository repository;
    private final CourseRepository courseRepository;

    public CourseSessionController(CourseSessionRepository repository, CourseRepository courseRepository) {
        this.repository = repository;
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public ApiResponse<PageDto<CourseSessionDto>> list(Pageable pageable) {
        Page<CourseSession> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, CourseSessionDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseSessionDto> get(@PathVariable Long id) {
        CourseSession entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "课程节次不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(CourseSessionDto.from(entity));
    }

    @PostMapping
    public ApiResponse<CourseSessionDto> create(@Valid @RequestBody CourseSession session) {
        checkCourseOwnership(session.getCourseId());
        return ApiResponse.success("创建成功", CourseSessionDto.from(repository.save(session)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseSessionDto> update(@PathVariable Long id, @Valid @RequestBody CourseSession session) {
        CourseSession existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "课程节次不存在: id=" + id));
        checkOwnership(existing);
        checkCourseOwnership(session.getCourseId());
        existing.setWeekday(session.getWeekday());
        existing.setStartTime(session.getStartTime());
        existing.setEndTime(session.getEndTime());
        existing.setStartWeek(session.getStartWeek());
        existing.setEndWeek(session.getEndWeek());
        existing.setWeekType(session.getWeekType());
        return ApiResponse.success("更新成功", CourseSessionDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CourseSession existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "课程节次不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    /** 单条数据归属校验：节次所属课程（再到学期）必须属于当前用户 */
    private void checkOwnership(CourseSession entity) {
        if (CurrentUserContext.isLogin()
                && !repository.existsByIdAndUserId(entity.getId(), CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的课程节次");
        }
    }

    /** 创建校验：目标课程必须属于当前用户 */
    private void checkCourseOwnership(Long courseId) {
        if (!CurrentUserContext.isLogin()) {
            return;
        }
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(404, "关联课程不存在: id=" + courseId);
        }
        if (!courseRepository.existsByIdAndUserId(courseId, CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权在他人课程下创建节次");
        }
    }
}
