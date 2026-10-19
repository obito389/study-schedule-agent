package com.studyplanner.business;

import com.studyplanner.enums.ActionStatus;
import com.studyplanner.enums.ActionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Agent动作实体（agent_action 表）
 */
@Entity
@Table(name = "agent_action")
public class AgentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "operation_key", nullable = false, length = 64)
    @NotBlank(message = "操作key不能为空")
    @Size(max = 64, message = "操作key不能超过64个字符")
    private String operationKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @NotNull(message = "动作类型不能为空")
    private ActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "状态不能为空")
    private ActionStatus status = ActionStatus.PREVIEW;

    @Column(name = "base_revision", nullable = false)
    @NotNull(message = "基础版本号不能为空")
    private Long baseRevision = 0L;

    @Column(name = "applied_revision", nullable = false)
    @NotNull(message = "应用版本号不能为空")
    private Long appliedRevision = 0L;

    /** 来源排程草案ID：批量撤销（按 plan_draft_id 整体回滚）的分组锚点 */
    @Column(name = "plan_draft_id")
    private Long planDraftId;

    @Column(name = "base_profile_version", nullable = false)
    @NotNull(message = "基础画像版本不能为空")
    private Long baseProfileVersion = 0L;

    @Column(name = "authorization_json", columnDefinition = "json")
    private String authorizationJson;

    @Column(name = "before_json", columnDefinition = "json")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "json")
    private String afterJson;

    @Column(name = "error_code", length = 32)
    @Size(max = 32, message = "错误码不能超过32个字符")
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }

    public AgentAction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }
    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }
    public ActionStatus getStatus() { return status; }
    public void setStatus(ActionStatus status) { this.status = status; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public Long getAppliedRevision() { return appliedRevision; }
    public void setAppliedRevision(Long appliedRevision) { this.appliedRevision = appliedRevision; }
    public Long getPlanDraftId() { return planDraftId; }
    public void setPlanDraftId(Long planDraftId) { this.planDraftId = planDraftId; }
    public Long getBaseProfileVersion() { return baseProfileVersion; }
    public void setBaseProfileVersion(Long baseProfileVersion) { this.baseProfileVersion = baseProfileVersion; }
    public String getAuthorizationJson() { return authorizationJson; }
    public void setAuthorizationJson(String authorizationJson) { this.authorizationJson = authorizationJson; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String beforeJson) { this.beforeJson = beforeJson; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
