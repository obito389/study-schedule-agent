package com.studyplanner.executor;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import com.studyplanner.common.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动作执行器接口（Agent 落地的统一入口）。
 * <ul>
 *   <li>POST /api/v1/executor/preview 执行前预览，不落库；</li>
 *   <li>POST /api/v1/executor/apply   确认执行，单事务批量落地。</li>
 * </ul>
 * 必须携带请求头 X-User-Id，且请求体 userId 与之一致。
 */
@RestController
@RequestMapping("/api/v1/executor")
public class ActionExecutorController {

    private final ActionExecutor executor;
    private final ActionAuditService auditService;

    public ActionExecutorController(ActionExecutor executor, ActionAuditService auditService) {
        this.executor = executor;
        this.auditService = auditService;
    }

    @PostMapping("/preview")
    public ApiResponse<ActionPreviewResult> preview(@Valid @RequestBody ActionBatchRequest request) {
        resolveUserId(request);
        return ApiResponse.success("预览成功", executor.preview(request));
    }

    @PostMapping("/apply")
    public ApiResponse<ActionApplyResult> apply(@Valid @RequestBody ActionBatchRequest request) {
        resolveUserId(request);
        ActionApplyResult result;
        try {
            result = executor.apply(request);
        } catch (BusinessException e) {
            // 走到这里 executor.apply 的事务已回滚、app_user 悲观锁已释放，
            // 此时再用独立事务（REQUIRES_NEW）记 FAILED 不会发生锁等待
            if (e.getCode() == ErrorCode.ACTION_EXECUTE_FAILED.getCode()) {
                recordFailureQuietly(request, "CONSTRAINT");
            }
            throw e;
        } catch (RuntimeException e) {
            // 执行期未预期异常（如数据库约束冲突）：整批已回滚，记 FAILED 便于排障
            recordFailureQuietly(request, "ERROR");
            throw e;
        }
        String message = result.isIdempotent() ? "幂等命中，未重复执行" : "执行成功";
        return ApiResponse.success(message, result);
    }

    /** 失败审计仅用于排障，审计本身失败时不能掩盖原始异常 */
    private void recordFailureQuietly(ActionBatchRequest request, String errorCode) {
        try {
            auditService.recordFailure(request.getUserId(), request, errorCode);
        } catch (RuntimeException ignored) {
            // 审计失败不阻断原始错误返回
        }
    }

    /** 以请求头登录态为准，校验/回填请求体 userId */
    private void resolveUserId(ActionBatchRequest request) {
        if (!CurrentUserContext.isLogin()) {
            throw new BusinessException(401, "请先登录（请求头携带 X-User-Id）");
        }
        Long headerUserId = CurrentUserContext.getUserId();
        if (request.getUserId() == null) {
            request.setUserId(headerUserId);
        } else if (!headerUserId.equals(request.getUserId())) {
            throw new BusinessException(403, "请求体 userId 与登录用户不一致");
        }
    }
}
