package com.studyplanner.dto;

import com.studyplanner.business.PlanDraft;
import com.studyplanner.enums.DraftStatus;
import java.time.LocalDateTime;

public class PlanDraftDto {
    private Long id;
    private Long userId;
    private Long baseRevision;
    private Long baseProfileVersion;
    private DraftStatus status;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private String proposalJson;
    private String requestKey;
    private LocalDateTime createdAt;
    private LocalDateTime appliedAt;

    public static PlanDraftDto from(PlanDraft entity) {
        PlanDraftDto dto = new PlanDraftDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setBaseRevision(entity.getBaseRevision());
        dto.setBaseProfileVersion(entity.getBaseProfileVersion());
        dto.setStatus(entity.getStatus());
        dto.setRangeStart(entity.getRangeStart());
        dto.setRangeEnd(entity.getRangeEnd());
        dto.setProposalJson(entity.getProposalJson());
        dto.setRequestKey(entity.getRequestKey());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setAppliedAt(entity.getAppliedAt());
        return dto;
    }

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
