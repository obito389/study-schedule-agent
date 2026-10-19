package com.studyplanner.business;

import com.studyplanner.enums.WeekType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalTime;

/**
 * 课程节次实体（course_session 表）
 */
@Entity
@Table(name = "course_session")
public class CourseSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @Column(name = "weekday", nullable = false)
    @NotNull(message = "星期几不能为空")
    @Min(value = 1, message = "星期最小为1")
    @Max(value = 7, message = "星期最大为7")
    private Integer weekday;

    @Column(name = "start_time", nullable = false)
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    @Column(name = "start_week", nullable = false)
    @NotNull(message = "开始周不能为空")
    @Min(value = 1, message = "开始周最小为1")
    private Integer startWeek;

    @Column(name = "end_week", nullable = false)
    @NotNull(message = "结束周不能为空")
    @Min(value = 1, message = "结束周最小为1")
    private Integer endWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_type", nullable = false, length = 8)
    @NotNull(message = "周类型不能为空")
    private WeekType weekType = WeekType.ALL;

    public CourseSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Integer getWeekday() { return weekday; }
    public void setWeekday(Integer weekday) { this.weekday = weekday; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getStartWeek() { return startWeek; }
    public void setStartWeek(Integer startWeek) { this.startWeek = startWeek; }
    public Integer getEndWeek() { return endWeek; }
    public void setEndWeek(Integer endWeek) { this.endWeek = endWeek; }
    public WeekType getWeekType() { return weekType; }
    public void setWeekType(WeekType weekType) { this.weekType = weekType; }
}
