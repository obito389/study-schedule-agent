package com.studyplanner.business;

import com.studyplanner.enums.WindowKind;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 可用时间窗口实体（availability_window 表）
 */
@Entity
@Table(name = "availability_window")
public class AvailabilityWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull(message = "窗口类型不能为空")
    private WindowKind kind;

    private Integer weekday;

    @Column(name = "local_date")
    private LocalDate localDate;

    @Column(name = "start_time", nullable = false)
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    public AvailabilityWindow() {}

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
