package com.studyplanner.business;

import com.studyplanner.enums.DraftStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 计划草稿实体（plan_draft 表）
 */
@Entity
@Table(name = "plan_draft")
public class PlanDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "base_revision", nullable = false)
    @NotNull(message = "基础版本号不能为空")
    private Long baseRevision;

    @Column(name = "base_profile_version", nullable = false)
    @NotNull(message = "基础画像版本不能为空")
    private Long baseProfileVersion = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull(message = "状态不能为空")
    private DraftStatus status = DraftStatus.DRAFT;

    @Column(name = "range_start", nullable = false)
    @NotNull(message = "范围开始时间不能为空")
    private LocalDateTime rangeStart;

    @Column(name = "range_end", nullable = false)
    @NotNull(message = "范围结束时间不能为空")
    private LocalDateTime rangeEnd;

    @Column(name = "proposal_json", columnDefinition = "json")
    private String proposalJson;

    @Column(name = "request_key", nullable = false, length = 64)
    @NotBlank(message = "请求key不能为空")
    @Size(max = 64, message = "请求key不能超过64个字符")
    private String requestKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public PlanDraft() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public Long getBaseProfileVersion() { return baseProfileVersion; }
    public void setBaseProfileVersion(Long baseProfileVersion) { this.baseProfileVersion = baseProfileVersion; }
    public DraftStatus getStatus() { return status; }
    public void setStatus(DraftStatus status) { this.status = status; }
    public LocalDateTime getRangeStart() { return rangeStart; }
    public void setRangeStart(LocalDateTime rangeStart) { this.rangeStart = rangeStart; }
    public LocalDateTime getRangeEnd() { return rangeEnd; }
    public void setRangeEnd(LocalDateTime rangeEnd) { this.rangeEnd = rangeEnd; }
    public String getProposalJson() { return proposalJson; }
    public void setProposalJson(String proposalJson) { this.proposalJson = proposalJson; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
