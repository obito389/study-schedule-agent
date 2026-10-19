package com.studyplanner.dto;

import com.studyplanner.business.Semester;
import java.time.LocalDate;

public class SemesterDto {
    private Long id;
    private Long userId;
    private String name;
    private LocalDate startDate;
    private Integer weekCount;

    public static SemesterDto from(Semester entity) {
        SemesterDto dto = new SemesterDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setName(entity.getName());
        dto.setStartDate(entity.getStartDate());
        dto.setWeekCount(entity.getWeekCount());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public Integer getWeekCount() { return weekCount; }
    public void setWeekCount(Integer weekCount) { this.weekCount = weekCount; }
}
