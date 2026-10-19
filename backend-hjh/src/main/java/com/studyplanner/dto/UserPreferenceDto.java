package com.studyplanner.dto;

import com.studyplanner.business.UserPreference;

public class UserPreferenceDto {
    private Long userId;
    private Integer dailyLimitMin;
    private Integer blockMin;
    private Integer horizonDays;

    public static UserPreferenceDto from(UserPreference entity) {
        UserPreferenceDto dto = new UserPreferenceDto();
        dto.setUserId(entity.getUserId());
        dto.setDailyLimitMin(entity.getDailyLimitMin());
        dto.setBlockMin(entity.getBlockMin());
        dto.setHorizonDays(entity.getHorizonDays());
        return dto;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getDailyLimitMin() { return dailyLimitMin; }
    public void setDailyLimitMin(Integer dailyLimitMin) { this.dailyLimitMin = dailyLimitMin; }
    public Integer getBlockMin() { return blockMin; }
    public void setBlockMin(Integer blockMin) { this.blockMin = blockMin; }
    public Integer getHorizonDays() { return horizonDays; }
    public void setHorizonDays(Integer horizonDays) { this.horizonDays = horizonDays; }
}
