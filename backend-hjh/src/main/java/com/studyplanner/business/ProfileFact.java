package com.studyplanner.business;

import com.studyplanner.enums.FactSource;
import com.studyplanner.enums.FactState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户画像事实实体（profile_fact 表）
 */
@Entity
@Table(name = "profile_fact")
public class ProfileFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(nullable = false, length = 64)
    @NotBlank(message = "事实key不能为空")
    @Size(max = 64, message = "事实key不能超过64个字符")
    private String key;

    @Column(name = "value_json", columnDefinition = "json")
    private String valueJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull(message = "来源不能为空")
    private FactSource source;

    @Column(name = "scope_json", columnDefinition = "json")
    private String scopeJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull(message = "状态不能为空")
    private FactState state = FactState.CANDIDATE;

    @Column(nullable = false, precision = 4, scale = 3)
    @NotNull(message = "置信度不能为空")
    private BigDecimal confidence = new BigDecimal("0.000");

    @Column(name = "evidence_ids", columnDefinition = "json")
    private String evidenceIds;

    @Column(nullable = false)
    @NotNull(message = "版本号不能为空")
    private Long version = 0L;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ProfileFact() {}

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
