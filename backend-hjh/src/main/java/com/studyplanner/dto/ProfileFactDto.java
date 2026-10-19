package com.studyplanner.dto;

import com.studyplanner.business.ProfileFact;
import com.studyplanner.enums.FactSource;
import com.studyplanner.enums.FactState;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProfileFactDto {
    private Long id;
    private Long userId;
    private String key;
    private String valueJson;
    private FactSource source;
    private String scopeJson;
    private FactState state;
    private BigDecimal confidence;
    private String evidenceIds;
    private Long version;
    private LocalDateTime expiresAt;
    private LocalDateTime updatedAt;

    public static ProfileFactDto from(ProfileFact entity) {
        ProfileFactDto dto = new ProfileFactDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setKey(entity.getKey());
        dto.setValueJson(entity.getValueJson());
        dto.setSource(entity.getSource());
        dto.setScopeJson(entity.getScopeJson());
        dto.setState(entity.getState());
        dto.setConfidence(entity.getConfidence());
        dto.setEvidenceIds(entity.getEvidenceIds());
        dto.setVersion(entity.getVersion());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValueJson() { return valueJson; }
    public void setValueJson(String valueJson) { this.valueJson = valueJson; }
    public FactSource getSource() { return source; }
    public void setSource(FactSource source) { this.source = source; }
    public String getScopeJson() { return scopeJson; }
    public void setScopeJson(String scopeJson) { this.scopeJson = scopeJson; }
    public FactState getState() { return state; }
    public void setState(FactState state) { this.state = state; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getEvidenceIds() { return evidenceIds; }
    public void setEvidenceIds(String evidenceIds) { this.evidenceIds = evidenceIds; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
