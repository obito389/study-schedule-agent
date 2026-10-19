package com.studyplanner.team;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.studyplanner.common.ApiException;
@RestControllerAdvice @Order(-100) public class TeamExceptionAdvice {
 @ExceptionHandler(ApiException.class) public ResponseEntity<?> provider(ApiException e){return ResponseEntity.status(e.httpStatus).body(Map.of("code",e.code,"message",e.getMessage()));}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> argument(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message",e.getMessage()==null?"参数无效":e.getMessage()));}
}
