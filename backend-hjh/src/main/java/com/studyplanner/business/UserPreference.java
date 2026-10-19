package com.studyplanner.business;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * 用户偏好实体（user_preference 表，主键是 user_id）
 */
@Entity
@Table(name = "user_preference")
public class UserPreference {

    @Id
    @Column(name = "user_id")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "daily_limit_min", nullable = false)
    @NotNull(message = "每日上限不能为空")
    @Min(value = 1, message = "每日上限至少为1分钟")
    private Integer dailyLimitMin = 120;

    @Column(name = "block_min", nullable = false)
    @NotNull(message = "时间块长度不能为空")
    @Min(value = 5, message = "时间块长度至少为5分钟")
    private Integer blockMin = 30;

    @Column(name = "horizon_days", nullable = false)
    @NotNull(message = "规划天数不能为空")
    @Min(value = 1, message = "规划天数至少为1天")
    private Integer horizonDays = 14;

    public UserPreference() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getDailyLimitMin() { return dailyLimitMin; }
    public void setDailyLimitMin(Integer dailyLimitMin) { this.dailyLimitMin = dailyLimitMin; }
    public Integer getBlockMin() { return blockMin; }
    public void setBlockMin(Integer blockMin) { this.blockMin = blockMin; }
    public Integer getHorizonDays() { return horizonDays; }
    public void setHorizonDays(Integer horizonDays) { this.horizonDays = horizonDays; }
}
