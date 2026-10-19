package com.studyplanner.undo;

import com.studyplanner.common.ApiResponse;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 版本与撤销接口（第 4 周）。
 * <ul>
 *   <li>POST /api/v1/undo/action  单步撤销（按 operation_key 回滚）</li>
 *   <li>POST /api/v1/undo/batch   批量撤销（按 plan_draft_id 整体回滚）</li>
 *   <li>GET  /api/v1/undo/history 历史版本查询（before_json / after_json 快照链）</li>
 * </ul>
 * 必须携带请求头 X-User-Id，且请求体 userId 与之一致。
 */
@RestController
@RequestMapping("/api/v1/undo")
public class UndoController {

    private final UndoExecutor undoExecutor;

    public UndoController(UndoExecutor undoExecutor) {
        this.undoExecutor = undoExecutor;
    }

    @PostMapping("/action")
    public ApiResponse<UndoResult> undoAction(@Valid @RequestBody UndoRequest request) {
        resolveUserId(request);
        return ApiResponse.success("撤销成功", undoExecutor.undoOne(request));
    }

    @PostMapping("/batch")
    public ApiResponse<UndoResult> undoBatch(@Valid @RequestBody UndoRequest request) {
        resolveUserId(request);
        return ApiResponse.success("批量撤销成功", undoExecutor.undoBatch(request));
    }

    @GetMapping("/history")
    public ApiResponse<List<UndoHistoryItem>> history(@RequestParam(required = false) Long planDraftId) {
        Long userId = requireLogin();
        return ApiResponse.success(undoExecutor.history(userId, planDraftId));
    }

    /** 以请求头登录态为准，校验/回填请求体 userId */
    private void resolveUserId(UndoRequest request) {
        Long headerUserId = requireLogin();
        if (request.getUserId() == null) {
            request.setUserId(headerUserId);
        } else if (!headerUserId.equals(request.getUserId())) {
            throw new BusinessException(403, "请求体 userId 与登录用户不一致");
        }
    }

    private Long requireLogin() {
        if (!CurrentUserContext.isLogin()) {
            throw new BusinessException(401, "请先登录（请求头携带 X-User-Id）");
        }
        return CurrentUserContext.getUserId();
    }
}
