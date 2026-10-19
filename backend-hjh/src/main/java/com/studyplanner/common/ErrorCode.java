package com.studyplanner.common;

/**
 * 统一业务错误码
 * 分段规则：
 *   0     成功
 *   400   请求参数错误
 *   401   未认证 / 未登录
 *   403   无权限访问
 *   404   资源不存在
 *   409   资源冲突（版本冲突 / 重复提交）
 *   500   服务器内部错误
 *   1xxx  用户/账户模块
 *   2xxx  课程/学期模块
 *   3xxx  任务/时间块模块
 *   4xxx  执行器/导入模块
 */
public enum ErrorCode {

    // 通用
    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突，请刷新后重试"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_DUPLICATED(1002, "用户名已存在"),
    PASSWORD_WRONG(1003, "密码错误"),

    // 课程模块 2xxx
    SEMESTER_NOT_FOUND(2001, "学期不存在"),
    COURSE_NOT_FOUND(2002, "课程不存在"),
    COURSE_SESSION_NOT_FOUND(2003, "课程节次不存在"),

    // 任务模块 3xxx
    TASK_NOT_FOUND(3001, "任务不存在"),
    TASK_STEP_NOT_FOUND(3002, "任务步骤不存在"),
    TIME_BLOCK_NOT_FOUND(3003, "时间块不存在"),
    TASK_VERSION_CONFLICT(3004, "任务版本已变更，请刷新后重试"),

    // 执行器模块 4xxx
    ACTION_EXECUTE_FAILED(4001, "动作执行失败"),
    IMPORT_FAILED(4002, "导入失败"),
    IDEMPOTENT_DUPLICATE(4003, "重复提交，请勿重复操作"),

    // 撤销模块 4xxx
    ACTION_NOT_UNDOABLE(4005, "该动作当前状态不可撤销"),
    UNDO_NOT_ALLOWED(4006, "已完成或已锁定的数据不可撤销"),
    NO_ACTION_TO_UNDO(4007, "没有可撤销的动作");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
