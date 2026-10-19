package com.studyplanner.team;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.studyplanner.common.ApiResponse;
import com.studyplanner.contract.ActionSpec;
@RestController @RequestMapping("/api/v1") public class TeamActionController {
 private final TeamSnapshotService snapshots;private final TeamActionService actions;private final TeamDraftService drafts;
 public TeamActionController(TeamSnapshotService snapshots,TeamActionService actions,TeamDraftService drafts){this.snapshots=snapshots;this.actions=actions;this.drafts=drafts;}
 @GetMapping("/health") public ApiResponse<?> health(){return ApiResponse.success(Map.of("status","UP","edition","team-assembly","aiConfigured",actions.configured(),"persistence","member-16-table-schema"));}
 @GetMapping("/schedule/snapshot") public ApiResponse<?> snapshot(){return ApiResponse.success(snapshots.snapshot());}
 public record Input(@NotBlank @Size(max=1500)String prompt,@NotNull @PositiveOrZero Long baseRevision){}
 @PostMapping("/actions") public ApiResponse<ActionSpec> preview(@Valid @RequestBody Input input){return ApiResponse.success(actions.preview(input.prompt(),input.baseRevision(),false));}
 public record Revision(@NotNull @PositiveOrZero Long baseRevision){}
 @PostMapping("/actions/plan") public ApiResponse<ActionSpec> plan(@Valid @RequestBody Revision input){return ApiResponse.success(actions.preview("规则排程",input.baseRevision(),true));}
 @PostMapping("/actions/{key}/confirm") public ApiResponse<ActionSpec> confirm(@PathVariable String key){return ApiResponse.success(drafts.confirm(key));}
 public record Undo(@NotBlank @Size(max=64)String operationKey,@NotBlank String executionId,@NotNull @PositiveOrZero Long baseRevision){}
 @PostMapping("/actions/undo") public ApiResponse<?> undo(@Valid @RequestBody Undo request){return ApiResponse.success(drafts.undo(request.executionId(),request.operationKey(),request.baseRevision()));}
}
