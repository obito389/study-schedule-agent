package com.studyplanner.team;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import com.studyplanner.common.ApiResponse;
@RestController @RequestMapping("/api/v1/workspace/tasks") public class TeamTaskController {
 private final TeamTaskService service;
 public TeamTaskController(TeamTaskService service){this.service=service;}
 public record Input(@NotBlank @Size(max=48)String operationKey,@NotNull @PositiveOrZero Long baseRevision,@NotBlank @Size(max=100)String title,@NotNull Instant deadline,@Min(1)@Max(10080)int durationMinutes,@Min(1)@Max(3)int priority,Long courseId){}
 public record Finish(@NotBlank @Size(max=48)String operationKey,@NotNull @PositiveOrZero Long baseRevision,@Min(1)@Max(10080)int actualMinutes){}
 public record Step(@NotBlank @Size(max=48)String operationKey,@NotNull @PositiveOrZero Long baseRevision,@NotBlank String title,@Min(1)int orderNo,@Min(1)int minutes){}
 @PostMapping public ApiResponse<?> create(@Valid @RequestBody Input input){return ApiResponse.success(service.save(null,input));}
 @PutMapping("/{id}")public ApiResponse<?> update(@PathVariable Long id,@Valid @RequestBody Input input){return ApiResponse.success(service.save(id,input));}
 @PostMapping("/{id}/complete")public ApiResponse<?> complete(@PathVariable Long id,@Valid @RequestBody Finish input){return ApiResponse.success(service.finish(id,input,false));}
 @PostMapping("/{id}/cancel")public ApiResponse<?> cancel(@PathVariable Long id,@Valid @RequestBody Finish input){return ApiResponse.success(service.finish(id,input,true));}
 @PostMapping("/{id}/steps")public ApiResponse<?> step(@PathVariable Long id,@Valid @RequestBody Step input){return ApiResponse.success(service.addStep(id,input));}
}
