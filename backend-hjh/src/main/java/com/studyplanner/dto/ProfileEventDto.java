package com.studyplanner.dto;

import com.studyplanner.business.ProfileEvent;
import java.time.LocalDateTime;

public class ProfileEventDto {
    private Long id;
    private Long userId;
    private String eventKey;
    private String type;
    private String payloadJson;
    private LocalDateTime createdAt;

    public static ProfileEventDto from(ProfileEvent entity) {
        ProfileEventDto dto = new ProfileEventDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setEventKey(entity.getEventKey());
        dto.setType(entity.getType());
        dto.setPayloadJson(entity.getPayloadJson());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
