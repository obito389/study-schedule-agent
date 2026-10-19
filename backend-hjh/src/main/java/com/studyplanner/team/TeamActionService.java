package com.studyplanner.team;
import java.util.*;
import java.time.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.studyplanner.agent.*;
import com.studyplanner.contract.*;
import com.studyplanner.executor.ActionBatchRequest;
import com.studyplanner.executor.ActionCommand;
import com.studyplanner.common.BusinessException;
@Service public class TeamActionService {
 private final AgentService agent;private final AgentToolRegistry tools;private final TeamSnapshotService snapshots;private final TeamDraftService drafts;private final TeamJson json;private final boolean configured;
 public TeamActionService(AgentService agent,AgentToolRegistry tools,TeamSnapshotService snapshots,TeamDraftService drafts,TeamJson json,@Value("${team.ai.api-key:}")String key){this.agent=agent;this.tools=tools;this.snapshots=snapshots;this.drafts=drafts;this.json=json;this.configured=key!=null&&!key.isBlank();}
 public boolean configured(){return configured;}
 public ActionSpec preview(String prompt,long revision,boolean rule){
  var snapshot=snapshots.snapshot();if(snapshot.planningRevision()!=revision)throw new BusinessException(409,"日程已更新，请刷新后重新生成");
  String key=UUID.randomUUID().toString();AgentResult result;
  if(rule)result=tools.execute(key,snapshot,new ModelAdapter.ModelResponse(ActionType.GENERATE_SCHEDULE,List.of(),Map.of(),List.of("规则排程"),List.of()));
  else {if(!configured)throw new BusinessException(503,"尚未配置 DeepSeek，仍可新建任务和使用规则排程");result=agent.handle(key,prompt,snapshot);}
  if(result.action()==null)return new ActionSpec(1,key,revision,ActionType.QUERY_SCHEDULE,List.of(),Map.of(),"schedule:read:self",List.of(result.message()),result.status(),List.of(new ContractError(result.status()==ActionStatus.FAILED?"MODEL_UNAVAILABLE":"NEEDS_CLARIFICATION",result.message(),List.of())));
  var a=result.action();if(a.status()==ActionStatus.NEEDS_CLARIFICATION||a.status()==ActionStatus.FAILED||a.type()==ActionType.QUERY_SCHEDULE)return a;
  List<ActionCommand> commands=new ArrayList<>();var changes=new LinkedHashMap<>(a.changes());
  switch(a.type()){
   case CREATE_TASK -> {var c=command(key,com.studyplanner.enums.ActionType.CREATE_TASK);var fields=a.changes();c.setTitle(fields.get("title").asText());c.setDeadline(TeamSnapshotService.local(Instant.parse(fields.get("deadline").asText())));
    c.setRemainingMin(fields.get("durationMinutes").asInt());c.setInitialEstimateMin(c.getRemainingMin());c.setPriority(Math.max(1,Math.min(3,fields.containsKey("priority")?fields.get("priority").asInt():2)));
    changes.put("priority",json.mapper.valueToTree(c.getPriority()));
    if(fields.containsKey("courseId")&&!fields.get("courseId").isNull())c.setCourseId(Long.parseLong(fields.get("courseId").asText()));
    if(fields.containsKey("start"))c.setStartAt(TeamSnapshotService.local(Instant.parse(fields.get("start").asText())));if(fields.containsKey("end"))c.setEndAt(TeamSnapshotService.local(Instant.parse(fields.get("end").asText())));commands.add(c);
   }
   case GENERATE_SCHEDULE -> {
    var candidate=result.scheduleCandidate();
    if(a.changes().containsKey("start")||a.changes().containsKey("end")||!a.targets().isEmpty()){
     Instant start=a.changes().containsKey("start")?Instant.parse(a.changes().get("start").asText()):snapshot.now();
     Instant end=a.changes().containsKey("end")?Instant.parse(a.changes().get("end").asText()):start.plus(Duration.ofDays(14));
     if(start.isBefore(snapshot.now())||!end.isAfter(start)||Duration.between(start,end).compareTo(Duration.ofDays(14))>0)throw new IllegalArgumentException("排程窗口应位于未来且不超过14天");
     for(var id:a.targets())if(snapshot.tasks().stream().noneMatch(t -> t.id().equals(id)))throw new IllegalArgumentException("指定任务不存在");
     var tasks=snapshot.tasks().stream().map(t -> new TaskSnapshot(t.id(),t.courseId(),t.title(),t.deadline().isBefore(end)?t.deadline():end,t.estimatedDurationMinutes(),t.priority(),a.targets().isEmpty()||a.targets().contains(t.id())?t.status():com.studyplanner.contract.TaskStatus.DONE,t.scheduledBlocks())).toList();
     var restricted=new ScheduleSnapshot(1,start,snapshot.timezone(),snapshot.courses(),tasks,snapshot.lockedBlocks(),snapshot.activeFacts(),snapshot.planningRevision());
     candidate=new com.studyplanner.planning.GreedyScheduler(new com.studyplanner.planning.HardConstraintValidator(),Clock.fixed(start,TeamSnapshotService.ZONE)).schedule(restricted);
    }
    if(candidate==null||candidate.blocks().isEmpty())return new ActionSpec(1,key,revision,a.type(),List.of(),Map.of(),a.authorizationScope(),List.of("没有可新增的完整安排，请调整截止时间或耗时"),ActionStatus.NEEDS_CLARIFICATION,List.of());
    changes.put("blocks",json.mapper.valueToTree(candidate.blocks()));changes.put("gaps",json.mapper.valueToTree(candidate.gaps()));
    int i=0;for(var block:candidate.blocks()){var c=command(key+"-"+(i++),com.studyplanner.enums.ActionType.CREATE_BLOCK);c.setTaskId(Long.valueOf(block.taskId()));c.setStartAt(TeamSnapshotService.local(block.start()));c.setEndAt(TeamSnapshotService.local(block.end()));commands.add(c);}
   }
   case MOVE_TASK -> {
    var task=snapshot.tasks().stream().filter(t -> t.id().equals(a.targets().getFirst())).findFirst().orElseThrow(() -> new BusinessException(404,"目标任务不存在"));
    if(task.scheduledBlocks().size()!=1)throw new BusinessException(422,"请对单个已排时间块操作");
    var c=command(key,com.studyplanner.enums.ActionType.UPDATE_BLOCK);c.setTargetId(Long.valueOf(task.scheduledBlocks().getFirst().id()));c.setStartAt(TeamSnapshotService.local(Instant.parse(a.changes().get("start").asText())));c.setEndAt(TeamSnapshotService.local(Instant.parse(a.changes().get("end").asText())));commands.add(c);
   }
   case CANCEL_TASK -> {var c=command(key,com.studyplanner.enums.ActionType.UPDATE_TASK);c.setTargetId(Long.valueOf(a.targets().getFirst()));c.setStatus(com.studyplanner.enums.TaskStatus.CANCELLED);commands.add(c);
    int i=0;for(var task:snapshot.tasks())if(task.id().equals(a.targets().getFirst()))for(var block:task.scheduledBlocks()){var b=command(key+"-"+(i++),com.studyplanner.enums.ActionType.UPDATE_BLOCK);b.setTargetId(Long.valueOf(block.id()));b.setBlockStatus(com.studyplanner.enums.BlockStatus.CANCELLED);commands.add(b);}}
   default -> throw new BusinessException(422,"暂不支持该动作");
  }
  var mapped=new ActionSpec(1,key,revision,a.type(),a.targets(),changes,a.authorizationScope(),a.reasonFacts(),ActionStatus.NEEDS_CONFIRMATION,List.of());
  var batch=new ActionBatchRequest();batch.setActions(commands);batch.setAuthorization("USER_CONFIRMED_SELF");return drafts.prepare(mapped,batch);
 }
 public static ActionCommand command(String key,com.studyplanner.enums.ActionType type){var c=new ActionCommand();c.setOperationKey(key);c.setType(type);return c;}
}
