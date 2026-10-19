package com.studyplanner.dto;

import com.studyplanner.business.AgentAction;
import com.studyplanner.enums.ActionStatus;
import com.studyplanner.enums.ActionType;
import java.time.LocalDateTime;

public class AgentActionDto {
    private Long id;
    private Long userId;
    private String operationKey;
    private ActionType type;
    private ActionStatus status;
    private Long baseRevision;
    private Long appliedRevision;
    private Long baseProfileVersion;
    private String authorizationJson;
    private String beforeJson;
    private String afterJson;
    private String errorCode;
    private LocalDateTime createdAt;

    public static AgentActionDto from(AgentAction entity) {
        AgentActionDto dto = new AgentActionDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setOperationKey(entity.getOperationKey());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setBaseRevision(entity.getBaseRevision());
        dto.setAppliedRevision(entity.getAppliedRevision());
        dto.setBaseProfileVersion(entity.getBaseProfileVersion());
        dto.setAuthorizationJson(entity.getAuthorizationJson());
        dto.setBeforeJson(entity.getBeforeJson());
        dto.setAfterJson(entity.getAfterJson());
        dto.setErrorCode(entity.getErrorCode());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

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
