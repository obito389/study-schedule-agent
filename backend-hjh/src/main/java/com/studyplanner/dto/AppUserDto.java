package com.studyplanner.dto;

import com.studyplanner.business.AppUser;
import java.time.LocalDateTime;

/**
 * 用户 DTO（隐藏 passwordHash），并透出方案/画像版本号，便于乐观锁与前端展示
 */
public class AppUserDto {

    private Long id;
    private String username;
    private String timezone;
    private Boolean learningEnabled;
    private Long planningRevision;
    private Long profileVersion;
    private LocalDateTime createdAt;

    public static AppUserDto from(AppUser user) {
        AppUserDto dto = new AppUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setTimezone(user.getTimezone());
        dto.setLearningEnabled(user.getLearningEnabled());
        dto.setPlanningRevision(user.getPlanningRevision());
        dto.setProfileVersion(user.getProfileVersion());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Boolean getLearningEnabled() { return learningEnabled; }
    public void setLearningEnabled(Boolean learningEnabled) { this.learningEnabled = learningEnabled; }
    public Long getPlanningRevision() { return planningRevision; }
    public void setPlanningRevision(Long planningRevision) { this.planningRevision = planningRevision; }
    public Long getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Long profileVersion) { this.profileVersion = profileVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
