package com.studyplanner.dto;

import com.studyplanner.business.AvailabilityWindow;
import com.studyplanner.enums.WindowKind;
import java.time.LocalDate;
import java.time.LocalTime;

public class AvailabilityWindowDto {
    private Long id;
    private Long userId;
    private WindowKind kind;
    private Integer weekday;
    private LocalDate localDate;
    private LocalTime startTime;
    private LocalTime endTime;

    public static AvailabilityWindowDto from(AvailabilityWindow entity) {
        AvailabilityWindowDto dto = new AvailabilityWindowDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setKind(entity.getKind());
        dto.setWeekday(entity.getWeekday());
        dto.setLocalDate(entity.getLocalDate());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public WindowKind getKind() { return kind; }
    public void setKind(WindowKind kind) { this.kind = kind; }
    public Integer getWeekday() { return weekday; }
    public void setWeekday(Integer weekday) { this.weekday = weekday; }
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate localDate) { this.localDate = localDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
