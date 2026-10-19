package com.studyplanner.business;

import com.studyplanner.enums.Modality;
import com.studyplanner.enums.ImportState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 导入任务实体（import_job 表）
 */
@Entity
@Table(name = "import_job")
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "request_key", nullable = false, length = 64)
    @NotBlank(message = "请求key不能为空")
    @Size(max = 64, message = "请求key不能超过64个字符")
    private String requestKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @NotNull(message = "模态类型不能为空")
    private Modality modality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "状态不能为空")
    private ImportState state = ImportState.UPLOADED;

    @Column(name = "source_ref", length = 255)
    @Size(max = 255, message = "来源引用不能超过255个字符")
    private String sourceRef;

    @Column(name = "source_hash", length = 64)
    @Size(max = 64, message = "来源哈希不能超过64个字符")
    private String sourceHash;

    @Column(name = "proposal_json", columnDefinition = "json")
    private String proposalJson;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "base_revision", nullable = false)
    @NotNull(message = "基础版本号不能为空")
    private Long baseRevision = 0L;

    @Column(nullable = false)
    @NotNull(message = "版本号不能为空")
    private Long version = 0L;

    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    @Column(name = "error_code", length = 32)
    @Size(max = 32, message = "错误码不能超过32个字符")
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public ImportJob() {}

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
