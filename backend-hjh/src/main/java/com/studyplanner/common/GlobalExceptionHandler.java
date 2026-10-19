package com.studyplanner.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 * 统一返回 ApiResponse，并保证 HTTP 状态码与业务 code 保持一致
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：按业务 code 映射 HTTP 状态码（404 / 403 / 401 / 400 ...）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(resolveStatus(e.getCode()))
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验失败（@Valid / @NotBlank / @Size / @Min / @Max 等）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    /**
     * 请求体 JSON 解析失败（包含传入非法枚举值，如 status="ABC"）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        String message = "请求体格式错误";
        String detail = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : null;
        if (detail != null && detail.contains("not one of the values accepted")) {
            message = "传入了非法的枚举值";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message));
    }

    /**
     * 兜底：未预期的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误: " + e.getMessage()));
    }

    /**
     * 业务 code 与 HTTP 状态码映射
     */
    private HttpStatus resolveStatus(int code) {
        if (code == 404) {
            return HttpStatus.NOT_FOUND;
        }
        if (code == 403) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == 401) {
            return HttpStatus.UNAUTHORIZED;
        }
        // 乐观锁版本冲突(3004)、幂等重复提交(4003) 统一返回 409 Conflict
        if (code == 409
                || code == ErrorCode.TASK_VERSION_CONFLICT.getCode()
                || code == ErrorCode.IDEMPOTENT_DUPLICATE.getCode()) {
            return HttpStatus.CONFLICT;
        }
        // 4 位业务错误码（1xxx~4xxx）均为客户端错误，默认 400
        if (code >= 1000) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code >= 400 && code < 500) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code >= 500) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }
}
