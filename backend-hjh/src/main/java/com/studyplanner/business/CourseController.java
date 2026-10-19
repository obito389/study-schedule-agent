package com.studyplanner.business;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.dto.CourseDto;
import com.studyplanner.dto.PageDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 课程 CRUD 接口
 * course 通过 semester_id → semester.user_id 间接归属用户
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseRepository repository;
    private final SemesterRepository semesterRepository;

    public CourseController(CourseRepository repository, SemesterRepository semesterRepository) {
        this.repository = repository;
        this.semesterRepository = semesterRepository;
    }

    @GetMapping
    public ApiResponse<PageDto<CourseDto>> list(Pageable pageable) {
        Page<Course> page = CurrentUserContext.isLogin()
                ? repository.findByUserId(CurrentUserContext.getUserId(), pageable)
                : repository.findAll(pageable);
        return ApiResponse.success(PageDto.from(page, CourseDto::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseDto> get(@PathVariable Long id) {
        Course entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "课程不存在: id=" + id));
        checkOwnership(entity);
        return ApiResponse.success(CourseDto.from(entity));
    }

    @PostMapping
    public ApiResponse<CourseDto> create(@Valid @RequestBody Course course) {
        checkSemesterOwnership(course.getSemesterId());
        return ApiResponse.success("创建成功", CourseDto.from(repository.save(course)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseDto> update(@PathVariable Long id, @Valid @RequestBody Course course) {
        Course existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "课程不存在: id=" + id));
        checkOwnership(existing);
        existing.setTitle(course.getTitle());
        existing.setTeacher(course.getTeacher());
        existing.setLocation(course.getLocation());
        return ApiResponse.success("更新成功", CourseDto.from(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Course existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "课程不存在: id=" + id));
        checkOwnership(existing);
        repository.delete(existing);
        return ApiResponse.success("删除成功", null);
    }

    /** 单条数据归属校验：课程所属学期必须属于当前用户 */
    private void checkOwnership(Course entity) {
        if (CurrentUserContext.isLogin()
                && !repository.existsByIdAndUserId(entity.getId(), CurrentUserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的课程");
        }
    }

    /** 创建校验：目标学期必须属于当前用户 */
    private void checkSemesterOwnership(Long semesterId) {
        if (!CurrentUserContext.isLogin()) {
            return;
        }
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException(404, "关联学期不存在: id=" + semesterId));
        if (!CurrentUserContext.getUserId().equals(semester.getUserId())) {
            throw new BusinessException(403, "无权在他人学期下创建课程");
        }
    }
}
