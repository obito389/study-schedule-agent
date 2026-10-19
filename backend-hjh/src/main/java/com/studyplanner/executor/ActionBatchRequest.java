package com.studyplanner.executor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 执行器批量动作请求：Agent 一次性下发的一组动作，在单事务内顺序执行。
 * baseRevision 为乐观锁版本，preview / apply 都需携带。
 */
public class ActionBatchRequest {

    /** 归属用户（也可由请求头 X-User-Id 提供，二者一致） */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 客户端生成方案时所基于的 planning_revision，用于乐观锁校验 */
    @NotNull(message = "基础版本号(baseRevision)不能为空")
    private Long baseRevision;

    /** 来源排程草案 ID（可选，用于时间块溯源） */
    private Long planDraftId;

    /** 用户授权信息（可选，原样记录到 agent_action.authorization_json） */
    private String authorization;

    /** 本批动作列表，按顺序执行 */
    @NotEmpty(message = "动作列表(actions)不能为空")
    @Valid
    private List<ActionCommand> actions;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public Long getPlanDraftId() { return planDraftId; }
    public void setPlanDraftId(Long planDraftId) { this.planDraftId = planDraftId; }
    public String getAuthorization() { return authorization; }
    public void setAuthorization(String authorization) { this.authorization = authorization; }
    public List<ActionCommand> getActions() { return actions; }
    public void setActions(List<ActionCommand> actions) { this.actions = actions; }
}
