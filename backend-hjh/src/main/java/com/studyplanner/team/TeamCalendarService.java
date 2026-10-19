package com.studyplanner.team;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import com.studyplanner.business.*;
import com.studyplanner.common.*;
import com.studyplanner.enums.*;

/** Versioned adapters over the member's semester/course/session/window entities. */
@org.springframework.stereotype.Service
public class TeamCalendarService {
    private final AppUserRepository users;
    private final CourseRepository courses;
    private final CourseSessionRepository sessions;
    private final SemesterRepository semesters;
    private final AvailabilityWindowRepository windows;
    private final TaskRepository tasks;
    private final TeamSnapshotService snapshots;
    private final TeamConstraintProjection constraints;
    public TeamCalendarService(AppUserRepository users, CourseRepository courses, CourseSessionRepository sessions,
            SemesterRepository semesters, AvailabilityWindowRepository windows, TaskRepository tasks,
            TeamSnapshotService snapshots, TeamConstraintProjection constraints) {
        this.users=users;this.courses=courses;this.sessions=sessions;this.semesters=semesters;
        this.windows=windows;this.tasks=tasks;this.snapshots=snapshots;this.constraints=constraints;
    }
    public record Input(@NotNull @PositiveOrZero Long baseRevision, @Size(max=100) String title,
            @NotNull LocalDate firstDate, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
            @Min(1) @Max(30) int weeks) {}
    public Object list() {
        Long user=TeamIdentity.user();
        return Map.of("courses",courses.findByUserId(user,Pageable.unpaged()).getContent().stream().map(c -> new CourseSummary(c.getId(),c.getTitle())).toList(),
                "windows",windows.findByUserId(user).stream().map(w -> new WindowSummary(w.getId(),w.getLocalDate(),w.getStartTime(),w.getEndTime())).toList());
    }
    public record CourseSummary(Long id,String title) {}
    public record WindowSummary(Long id,LocalDate localDate,LocalTime startTime,LocalTime endTime) {}
    private AppUser lock(long revision) {
        var user=users.findByIdForUpdate(TeamIdentity.user()).orElseThrow();
        if(user.getPlanningRevision()!=revision)throw new BusinessException(409,"日程已更新，请刷新后重试");
        return user;
    }
    private Object finish(AppUser user) {
        var snapshot=snapshots.snapshot();
        var issues=constraints.validateState(snapshot,snapshot.tasks());
        if(!issues.isEmpty())throw new BusinessException(422,String.join("；",issues));
        user.setPlanningRevision(user.getPlanningRevision()+1);users.save(user);
        return snapshots.snapshot();
    }
    private void check(Input input) {
        if(!input.endTime().isAfter(input.startTime()))throw new IllegalArgumentException("结束时间必须晚于开始时间");
        if(input.startTime().getSecond()!=0||input.endTime().getSecond()!=0)throw new IllegalArgumentException("请按分钟填写时间");
    }
    @Transactional public Object course(Input input) {
        var user=lock(input.baseRevision());check(input);
        if(input.title()==null||input.title().isBlank())throw new IllegalArgumentException("请填写课程名称");
        var semester=new Semester();semester.setUserId(user.getId());semester.setName("课程周期");
        semester.setStartDate(input.firstDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        semester.setWeekCount(input.weeks());semesters.saveAndFlush(semester);
        var course=new Course();course.setSemesterId(semester.getId());course.setTitle(input.title().trim());courses.saveAndFlush(course);
        var session=new CourseSession();session.setCourseId(course.getId());session.setWeekday(input.firstDate().getDayOfWeek().getValue());
        session.setStartTime(input.startTime());session.setEndTime(input.endTime());session.setStartWeek(1);session.setEndWeek(input.weeks());sessions.saveAndFlush(session);
        return finish(user);
    }
    @Transactional public Object window(Input input) {
        var user=lock(input.baseRevision());check(input);var window=new AvailabilityWindow();window.setUserId(user.getId());
        window.setKind(WindowKind.BUSY);window.setLocalDate(input.firstDate());window.setStartTime(input.startTime());window.setEndTime(input.endTime());
        windows.saveAndFlush(window);return finish(user);
    }
    @Transactional public Object removeCourse(Long id,long baseRevision) {
        var user=lock(baseRevision);var course=courses.findById(id).filter(c -> semesters.findById(c.getSemesterId()).orElseThrow().getUserId().equals(user.getId())).orElseThrow(() -> new BusinessException(404,"课程不存在"));
        if(tasks.findByUserId(user.getId()).stream().anyMatch(t -> id.equals(t.getCourseId())))throw new BusinessException(409,"课程仍有关联任务，不能删除");
        sessions.deleteAll(sessions.findByCourseId(id));courses.delete(course);courses.flush();
        return finish(user);
    }
    @Transactional public Object removeWindow(Long id,long baseRevision) {
        var user=lock(baseRevision);var window=windows.findById(id).filter(w -> w.getUserId().equals(user.getId())).orElseThrow(() -> new BusinessException(404,"时段不存在"));
        windows.delete(window);windows.flush();return finish(user);
    }
}
