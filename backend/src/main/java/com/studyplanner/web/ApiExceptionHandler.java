package com.studyplanner.web;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.studyplanner.common.ApiException;
@RestControllerAdvice(assignableTypes=PlannerController.class) @Order(-10)
public class ApiExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> known(ApiException e) { return ResponseEntity.status(e.httpStatus).body(Map.of("code",e.code,"message",e.getMessage())); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message","请求格式无效，请检查必填字段和日期时间"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> tooLarge(Exception e) {
        return ResponseEntity.status(413).body(Map.of("code","UPLOAD_TOO_LARGE","message","文件不能超过 10 MB"));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> argument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message",Objects.toString(e.getMessage(),"参数无效")));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) {
        String id=UUID.randomUUID().toString();
        // Never print request payloads or model/upload content in logs.
        LOG.error("Unexpected server error {} ({})",id,e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(Map.of("code","INTERNAL_ERROR","message","服务异常，请重试","correlationId",id));
    }
}
