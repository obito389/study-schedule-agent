package com.studyplanner.dto;

import com.studyplanner.business.CourseSession;
import com.studyplanner.enums.WeekType;
import java.time.LocalTime;

public class CourseSessionDto {
    private Long id;
    private Long courseId;
    private Integer weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer startWeek;
    private Integer endWeek;
    private WeekType weekType;

    public static CourseSessionDto from(CourseSession entity) {
        CourseSessionDto dto = new CourseSessionDto();
        dto.setId(entity.getId());
        dto.setCourseId(entity.getCourseId());
        dto.setWeekday(entity.getWeekday());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setStartWeek(entity.getStartWeek());
        dto.setEndWeek(entity.getEndWeek());
        dto.setWeekType(entity.getWeekType());
        return dto;
    }

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
