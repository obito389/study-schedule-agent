package com.studyplanner.business;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 用户画像事件实体（profile_event 表）
 */
@Entity
@Table(name = "profile_event")
public class ProfileEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "event_key", nullable = false, length = 64)
    @NotBlank(message = "事件key不能为空")
    @Size(max = 64, message = "事件key不能超过64个字符")
    private String eventKey;

    @Column(nullable = false, length = 32)
    @NotBlank(message = "事件类型不能为空")
    @Size(max = 32, message = "事件类型不能超过32个字符")
    private String type;

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public ProfileEvent() {}

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
