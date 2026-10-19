package com.studyplanner.delivery;

import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.studyplanner.contract.*;
import com.studyplanner.team.*;

@RestController @RequestMapping("/api/v1")
public class DeliveryController {
 private final DeliveryCommands commands;private final TeamSnapshotService snapshots;private final TeamActionService actions;
 private final TeamProfileService profile;private final TeamCalendarService calendar;private final TeamJson json;
 public DeliveryController(DeliveryCommands commands,TeamSnapshotService snapshots,TeamActionService actions,TeamProfileService profile,TeamCalendarService calendar,TeamJson json){this.commands=commands;this.snapshots=snapshots;this.actions=actions;this.profile=profile;this.calendar=calendar;this.json=json;}
 @GetMapping("/schedule") public ScheduleSnapshot schedule(){return snapshots.snapshot();}
 @GetMapping("/actions") public Object history(){return commands.history();}
 @PostMapping("/workspace/commands") public Object execute(@Valid @RequestBody DeliveryCommands.Command c){return commands.execute(c);}
 @GetMapping("/ai/status") public Object status(){return Map.of("configured",actions.configured(),"provider","DeepSeek","model","deepseek-flash","mode","TEAM_FINAL");}
 public record Instruction(@NotBlank @Size(max=1500)String instruction){}
 @PostMapping("/ai/previews") public Object ai(@Valid @RequestBody Instruction body){
  long start=System.nanoTime();var a=actions.preview(body.instruction(),snapshots.snapshot().planningRevision(),false);
  var candidate=candidate(a);return Map.ofEntries(Map.entry("draftId",a.operationKey()),Map.entry("status",a.status().name()),Map.entry("provider","DeepSeek"),Map.entry("model","deepseek-flash"),Map.entry("baseRevision",a.baseRevision()),Map.entry("elapsedMillis",(System.nanoTime()-start)/1000000),Map.entry("explanations",a.reasonFacts()),Map.entry("clarifications",a.errors().stream().map(ContractError::message).toList()),Map.entry("action",a),Map.entry("candidate",candidate));
 }
 private Map<String,Object> candidate(ActionSpec a){
  if(a.type()==ActionType.CREATE_TASK&&a.changes().containsKey("start"))return Map.of("blocks",List.of(Map.of("taskId","待创建","start",a.changes().get("start"),"end",a.changes().get("end"))),"gaps",List.of());
  return Map.of("blocks",a.changes().containsKey("blocks")?a.changes().get("blocks"):List.of(),"gaps",a.changes().containsKey("gaps")?a.changes().get("gaps"):List.of());
 }
 @PostMapping("/ai/drafts/{id}/confirm") public Object confirm(@PathVariable String id){var d=commands.owned(id);return commands.execute(new DeliveryCommands.Command(UUID.nameUUIDFromBytes(("confirm:"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),d.getBaseRevision(),"APPLY_PLAN","schedule:write:self",json.mapper.valueToTree(Map.of("previewId",id))));}
 @PostMapping("/planning/previews") public Object plan(){var a=actions.preview("规则排程",snapshots.snapshot().planningRevision(),true);return Map.of("previewId",a.operationKey(),"baseRevision",a.baseRevision(),"mode","RULE","candidate",candidate(a));}
 @GetMapping("/profile") public Object profile(){return Map.of("history",profile.latest());}
 public record Preference(@NotBlank String key,JsonNode value,@NotBlank String action,@PositiveOrZero long baseRevision){}
 @PostMapping("/profile/preferences") public Object preference(@Valid @RequestBody Preference p){if(!Set.of("SET","DENY","DELETE").contains(p.action()))throw new IllegalArgumentException("未知事实操作");return profile.change(p.key(),p.value(),p.action(),p.baseRevision(),null);}
 public record Fixed(@NotNull Instant start,@NotNull Instant end,@NotBlank @Size(max=100)String name,@NotBlank String kind,@PositiveOrZero long baseRevision){}
 @PostMapping("/workspace/fixed") public Object fixed(@Valid @RequestBody Fixed f){
  var start=TeamSnapshotService.local(f.start());var end=TeamSnapshotService.local(f.end());
  if(!start.toLocalDate().equals(end.toLocalDate()))throw new IllegalArgumentException("课程或固定时段请按同一天填写");
  var input=new TeamCalendarService.Input(f.baseRevision(),f.name(),start.toLocalDate(),start.toLocalTime(),end.toLocalTime(),1);
  if(f.kind().equals("COURSE"))return calendar.course(input);if(!Set.of("PERSONAL","UNAVAILABLE").contains(f.kind()))throw new IllegalArgumentException("时段类型无效");return calendar.window(input);
 }
 @DeleteMapping("/workspace/fixed/{kind}/{id}") public Object remove(@PathVariable String kind,@PathVariable String id,@RequestParam long baseRevision){
  if(kind.equals("COURSE"))return calendar.removeCourse(Long.valueOf(id),baseRevision);
  if(!id.startsWith("fixed-"))throw new IllegalArgumentException("仅可删除固定时段或课程");return calendar.removeWindow(Long.valueOf(id.split("-")[1]),baseRevision);
 }
}
