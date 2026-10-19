package com.studyplanner.web;
import java.time.Instant;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.business.*;
import com.studyplanner.contract.*;
import com.studyplanner.profile.ProfileService;
import com.studyplanner.agent.LiveAiService;
@RestController
@RequestMapping("/api/v1")
public class PlannerController {
    private final ScheduleStore store;
    private final ActionExecutor executor;
    private final PlanningService planning;
    private final CalendarService calendar;
    private final ProfileService profile;
    private final ObjectMapper mapper;
    private final LiveAiService ai;
    public PlannerController(ScheduleStore store,ActionExecutor executor,PlanningService planning,
            CalendarService calendar,ProfileService profile,ObjectMapper mapper,LiveAiService ai) {
        this.store=store;this.executor=executor;this.planning=planning;this.calendar=calendar;this.profile=profile;this.mapper=mapper;this.ai=ai;
    }
    @GetMapping("/health") public Map<String,Object> health() {
        return Map.of("status","UP","mode",ai.status().mode(),"aiProvider","DeepSeek","importMode",ai.status().configured()?"text-live-files-manual":"mock","user","demo","timezone","Asia/Shanghai");
    }
    @GetMapping("/schedule") public ScheduleSnapshot schedule() { return store.snapshot(); }
    @PostMapping("/actions") public ResponseEntity<ExecutionReceipt> execute(@RequestBody ActionCommand command) {
        ExecutionReceipt receipt=executor.execute(command);return ResponseEntity.status(receipt.httpStatus()).body(receipt);
    }
    @GetMapping("/actions") public List<ExecutionReceipt> history() { return executor.history(); }
    public record UndoInput(@NotBlank String operationKey,@PositiveOrZero long baseRevision) {}
    @PostMapping("/actions/{executionId}/undo")
    public ResponseEntity<ExecutionReceipt> undo(@PathVariable String executionId,@Valid @RequestBody UndoInput input) {
        return execute(new ActionCommand(input.operationKey(),input.baseRevision(),"UNDO","schedule:write:self",
                mapper.createObjectNode().put("executionId",executionId)));
    }
    @PostMapping("/planning/previews") public PlanningService.Preview preview() { return planning.preview(); }
    public record CourseInput(@NotBlank @Size(max=200) String name,@NotNull Instant start,@NotNull Instant end,
            @PositiveOrZero long baseRevision) {}
    @PostMapping("/courses") public ScheduleSnapshot course(@Valid @RequestBody CourseInput input) {
        return calendar.addCourse(input.name(),input.start(),input.end(),input.baseRevision());
    }
    @DeleteMapping("/courses/{id}") public ScheduleSnapshot deleteCourse(@PathVariable String id,@RequestParam long baseRevision) {
        return calendar.deleteCourse(id,baseRevision);
    }
    public record BlockInput(@NotBlank @Size(max=80) String title,@NotNull Instant start,@NotNull Instant end,
            boolean unavailable,@PositiveOrZero long baseRevision) {}
    @PostMapping("/blocks") public ScheduleSnapshot block(@Valid @RequestBody BlockInput input) {
        return calendar.addBlock(input.title(),input.start(),input.end(),input.unavailable(),input.baseRevision());
    }
    @DeleteMapping("/blocks/{id}") public ScheduleSnapshot deleteBlock(@PathVariable String id,@RequestParam long baseRevision) {
        return calendar.deleteBlock(id,baseRevision);
    }
    @GetMapping("/profile") public Map<String,Object> profile() {
        return Map.of("activeFacts",profile.activeFacts(),"history",profile.history(),"profileVersion",profile.profileVersion());
    }
    public record PreferenceInput(@NotBlank String key,JsonNode value,@NotBlank String action,@PositiveOrZero long baseRevision) {}
    @PostMapping("/profile/preferences") public List<ProfileFact> preference(@Valid @RequestBody PreferenceInput input) {
        return calendar.preference(input.key(),input.value(),input.action(),input.baseRevision());
    }
    @GetMapping("/profile/duration-estimate") public Object duration(@RequestParam(defaultValue="60") int originalMinutes) {
        if (originalMinutes<1 || originalMinutes>10080) throw new IllegalArgumentException("预计耗时应为 1 至 10080 分钟");
        return profile.estimateDuration("GLOBAL",originalMinutes,false);
    }
}
