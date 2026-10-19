package com.studyplanner.dto;

import com.studyplanner.business.ImportJob;
import com.studyplanner.enums.Modality;
import com.studyplanner.enums.ImportState;
import java.time.LocalDateTime;

public class ImportJobDto {
    private Long id;
    private Long userId;
    private String requestKey;
    private Modality modality;
    private ImportState state;
    private String sourceRef;
    private String sourceHash;
    private String proposalJson;
    private String transcript;
    private Long baseRevision;
    private Long version;
    private LocalDateTime leaseUntil;
    private String errorCode;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static ImportJobDto from(ImportJob entity) {
        ImportJobDto dto = new ImportJobDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setRequestKey(entity.getRequestKey());
        dto.setModality(entity.getModality());
        dto.setState(entity.getState());
        dto.setSourceRef(entity.getSourceRef());
        dto.setSourceHash(entity.getSourceHash());
        dto.setProposalJson(entity.getProposalJson());
        dto.setTranscript(entity.getTranscript());
        dto.setBaseRevision(entity.getBaseRevision());
        dto.setVersion(entity.getVersion());
        dto.setLeaseUntil(entity.getLeaseUntil());
        dto.setErrorCode(entity.getErrorCode());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public Modality getModality() { return modality; }
    public void setModality(Modality modality) { this.modality = modality; }
    public ImportState getState() { return state; }
    public void setState(ImportState state) { this.state = state; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
    public String getProposalJson() { return proposalJson; }
    public void setProposalJson(String proposalJson) { this.proposalJson = proposalJson; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(LocalDateTime leaseUntil) { this.leaseUntil = leaseUntil; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
