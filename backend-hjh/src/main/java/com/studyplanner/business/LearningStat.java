package com.studyplanner.business;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习统计实体（learning_stat 表）
 */
@Entity
@Table(name = "learning_stat")
public class LearningStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "task_type", nullable = false, length = 32)
    @NotBlank(message = "任务类型不能为空")
    @Size(max = 32, message = "任务类型不能超过32个字符")
    private String taskType;

    @Column(name = "sample_count", nullable = false)
    @NotNull(message = "样本数量不能为空")
    @Min(value = 0, message = "样本数量不能为负数")
    private Integer sampleCount = 0;

    @Column(nullable = false, precision = 5, scale = 3)
    @NotNull(message = "比率不能为空")
    private BigDecimal ratio = new BigDecimal("1.000");

    @Column(name = "last_event_id")
    private Long lastEventId;

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

    public LearningStat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
    public Long getLastEventId() { return lastEventId; }
    public void setLastEventId(Long lastEventId) { this.lastEventId = lastEventId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
