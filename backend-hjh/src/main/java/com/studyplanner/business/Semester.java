package com.studyplanner.business;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * 学期实体（semester 表）
 */
@Entity
@Table(name = "semester")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(nullable = false, length = 64)
    @NotBlank(message = "学期名称不能为空")
    @Size(max = 64, message = "学期名称不能超过64个字符")
    private String name;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Column(name = "week_count", nullable = false)
    @NotNull(message = "周数不能为空")
    @Min(value = 1, message = "周数至少为1")
    private Integer weekCount = 16;

    public Semester() {}

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
