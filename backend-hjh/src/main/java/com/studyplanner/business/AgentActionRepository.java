package com.studyplanner.business;

import com.studyplanner.enums.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AgentActionRepository extends JpaRepository<AgentAction, Long> {
    List<AgentAction> findByUserId(Long userId);
    List<AgentAction> findByUserIdAndStatus(Long userId, String status);
    Page<AgentAction> findByUserId(Long userId, Pageable pageable);

    /** 幂等：按 operationKey 查询单条动作 */
    Optional<AgentAction> findByUserIdAndOperationKey(Long userId, String operationKey);

    /** 幂等：批量查询一批 operationKey（用于整批提交前去重判断） */
    List<AgentAction> findByUserIdAndOperationKeyIn(Long userId, Collection<String> operationKeys);

    /** 撤销/版本追溯：按来源草案查询该批全部动作（批量撤销、历史版本查询用） */
    List<AgentAction> findByUserIdAndPlanDraftId(Long userId, Long planDraftId);

    /** 撤销：按来源草案 + 状态查询，倒序返回（批量撤销须逆序执行，见 UndoExecutor 说明） */
    List<AgentAction> findByUserIdAndPlanDraftIdAndStatusOrderByIdDesc(Long userId, Long planDraftId,
                                                                      ActionStatus status);
}
