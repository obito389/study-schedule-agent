package com.studyplanner.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.business.AppUserRepository;
import com.studyplanner.business.AgentAction;
import com.studyplanner.business.AgentActionRepository;
import com.studyplanner.enums.ActionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 动作失败审计服务。
 * 独立事务（REQUIRES_NEW）记录 FAILED 状态，保证 ActionExecutor 主事务回滚后，
 * agent_action 中的失败记录仍能持久化，便于排障与重试判断。
 */
@Service
public class ActionAuditService {

    private final AgentActionRepository agentActionRepository;

    private final AppUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ActionAuditService(AgentActionRepository agentActionRepository,
                              AppUserRepository userRepository, ObjectMapper objectMapper) {
        this.agentActionRepository = agentActionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long userId, ActionBatchRequest request, String errorCode) {
        // Same lock order as apply/undo; a late failure must not overwrite a successful retry.
        if (userRepository.findByIdForUpdate(userId).isEmpty()) return;
        String requestHash = RequestFingerprint.of(request, objectMapper);
        for (ActionCommand a : request.getActions()) {
            AgentAction action = agentActionRepository
                    .findByUserIdAndOperationKey(userId, a.getOperationKey())
                    .orElseGet(() -> {
                        AgentAction x = new AgentAction();
                        x.setUserId(userId);
                        x.setOperationKey(a.getOperationKey());
                        x.setType(a.getType());
                        return x;
                    });
            if (action.getStatus() == ActionStatus.APPLIED || action.getStatus() == ActionStatus.UNDONE
                    || (action.getId() != null && !requestHash.equals(action.getRequestHash()))) {
                continue;
            }
            action.setRequestHash(requestHash);
            action.setStatus(ActionStatus.FAILED);
            action.setBaseRevision(request.getBaseRevision());
            action.setAppliedRevision(request.getBaseRevision());
            action.setErrorCode(errorCode);
            action.setAfterJson(null);
            agentActionRepository.save(action);
        }
    }
}
