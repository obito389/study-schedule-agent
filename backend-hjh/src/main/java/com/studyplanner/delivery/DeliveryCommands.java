package com.studyplanner.delivery;

import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.business.*;
import com.studyplanner.common.BusinessException;
import com.studyplanner.enums.DraftStatus;
import com.studyplanner.executor.*;
import com.studyplanner.team.*;
import com.studyplanner.undo.*;

/** Uses the member's drafts and audit tables for final UI receipts. */
@Service
public class DeliveryCommands {
 public record Command(@NotBlank @Size(max=48) String operationKey,@PositiveOrZero long baseRevision,
   @NotBlank String type,String authorizationScope,@NotNull JsonNode payload) {}
 public record Receipt(String executionId,String operationKey,String type,String status,long planningRevision,
   int httpStatus,String message,List<?> errors,boolean undoable) {}
 public record Saved(Command command,Receipt receipt) {}
 private final AppUserRepository users;private final PlanDraftRepository drafts;private final AgentActionRepository audits;
 private final TimeBlockRepository blocks;private final TeamJson json;private final TeamTaskService tasks;
 private final TeamDraftService teamDrafts;private final ActionExecutor executor;private final UndoExecutor undo;private final Clock clock;
 public DeliveryCommands(AppUserRepository users,PlanDraftRepository drafts,AgentActionRepository audits,
   TimeBlockRepository blocks,TeamJson json,TeamTaskService tasks,TeamDraftService teamDrafts,
   ActionExecutor executor,UndoExecutor undo,Clock clock){
  this.users=users;this.drafts=drafts;this.audits=audits;this.blocks=blocks;this.json=json;this.tasks=tasks;
  this.teamDrafts=teamDrafts;this.executor=executor;this.undo=undo;this.clock=clock;
 }
 public PlanDraft owned(String key){return drafts.findByUserId(TeamIdentity.user()).stream().filter(d -> key.equals(d.getRequestKey())).findFirst().orElseThrow(() -> new BusinessException(404,"执行记录或草案不存在"));}
 private boolean hasSaved(PlanDraft d){return d.getProposalJson()!=null&&json.read(d.getProposalJson(),JsonNode.class).has("command");}
 private Receipt displayed(Receipt r){
  boolean active=drafts.findByUserId(TeamIdentity.user()).stream().anyMatch(d -> d.getRequestKey().equals(r.executionId())&&d.getStatus()==DraftStatus.APPLIED);
  return new Receipt(r.executionId(),r.operationKey(),r.type(),r.status(),r.planningRevision(),r.httpStatus(),r.undoable()&&!active?"此操作已撤销":r.message(),r.errors(),r.undoable()&&active);
 }
 public List<Receipt> history(){var result=new ArrayList<Receipt>();for(var d:drafts.findByUserId(TeamIdentity.user()).stream().sorted(Comparator.comparing(PlanDraft::getId).reversed()).toList())if(hasSaved(d))result.add(displayed(json.read(d.getProposalJson(),Saved.class).receipt()));return result;}
 @Transactional public Receipt execute(Command c){
  var user=users.findByIdForUpdate(TeamIdentity.user()).orElseThrow();
  var prior=drafts.findByUserId(user.getId()).stream().filter(d -> d.getRequestKey().equals(c.operationKey())).findFirst().orElse(null);
  if(prior!=null){
   if(!hasSaved(prior))throw new BusinessException(409,"操作键已被其他草案使用");var saved=json.read(prior.getProposalJson(),Saved.class);
   if(!saved.command().equals(c))throw new BusinessException(409,"操作键已绑定不同请求");
   if(saved.receipt().undoable()&&!displayed(saved.receipt()).undoable())throw new BusinessException(409,"该操作已经撤销");return saved.receipt();
  }
  if(user.getPlanningRevision()!=c.baseRevision())throw new BusinessException(409,"日程已更新，请刷新后重新确认");
  if(!"schedule:write:self".equals(c.authorizationScope()))throw new BusinessException(403,"操作范围无效");
  var p=c.payload();String execution=c.operationKey();boolean undoable=true;
  switch(c.type()){
   case "CREATE_TASK","UPDATE_TASK" -> tasks.save(c.type().equals("CREATE_TASK")?null:id(p,"taskId"),new TeamTaskController.Input(c.operationKey(),c.baseRevision(),text(p,"title"),Instant.parse(text(p,"deadline")),integer(p,"durationMinutes",1,10080),integer(p,"priority",1,3),p.hasNonNull("courseId")&&!p.path("courseId").asText().isBlank()?id(p,"courseId"):null));
   case "CANCEL_TASK","COMPLETE_TASK" -> {boolean cancel=c.type().equals("CANCEL_TASK");tasks.finish(id(p,"taskId"),new TeamTaskController.Finish(c.operationKey(),c.baseRevision(),cancel?1:integer(p,"actualMinutes",1,10080)),cancel);undoable=cancel;}
   case "MOVE_TASK" -> {
    var b=blocks.findById(id(p,"blockId")).filter(x -> x.getUserId().equals(user.getId())&&x.getTaskId().equals(id(p,"taskId"))).orElseThrow(() -> new BusinessException(404,"时间块不存在"));
    var a=TeamActionService.command(c.operationKey(),com.studyplanner.enums.ActionType.UPDATE_BLOCK);a.setTargetId(b.getId());a.setStartAt(TeamSnapshotService.local(Instant.parse(text(p,"start"))));a.setEndAt(TeamSnapshotService.local(Instant.parse(text(p,"end"))));apply(c,List.of(a));
   }
   case "APPLY_PLAN" -> {execution=text(p,"previewId");teamDrafts.confirm(execution);}
   case "UNDO" -> {
    undoable=false;var target=owned(text(p,"executionId"));
    if(hasSaved(target)){
     var saved=json.read(target.getProposalJson(),Saved.class);if(!saved.receipt().undoable()||target.getStatus()!=DraftStatus.APPLIED)throw new BusinessException(409,"此操作不能撤销");
     var r=new UndoRequest();r.setUserId(user.getId());r.setPlanDraftId(target.getId());r.setBaseRevision(c.baseRevision());undo.undoBatch(r);target.setStatus(DraftStatus.CANCELLED);drafts.save(target);
    }else teamDrafts.undo(target.getRequestKey(),c.operationKey(),c.baseRevision());
   }
   default -> throw new IllegalArgumentException("不支持的操作类型");
  }
  String receiptType=c.type();if(c.type().equals("APPLY_PLAN")){var source=json.read(owned(execution).getProposalJson(),JsonNode.class).path("action").path("type").asText();if(!source.equals("GENERATE_SCHEDULE"))receiptType=source;}
  var receipt=new Receipt(execution,c.operationKey(),receiptType,"APPLIED",user.getPlanningRevision(),200,"操作已保存",List.of(),undoable);
  var d=new PlanDraft();d.setUserId(user.getId());d.setBaseRevision(c.baseRevision());d.setBaseProfileVersion(user.getProfileVersion());d.setRequestKey(c.operationKey());d.setStatus(DraftStatus.APPLIED);
  d.setRangeStart(TeamSnapshotService.local(clock.instant()));d.setRangeEnd(TeamSnapshotService.local(clock.instant().plus(Duration.ofDays(14))));d.setProposalJson(json.write(new Saved(c,receipt)));drafts.saveAndFlush(d);
  if(!c.type().equals("APPLY_PLAN")&&!c.type().equals("UNDO"))for(var a:audits.findByUserId(user.getId()))if(a.getOperationKey().equals(c.operationKey())||a.getOperationKey().startsWith(c.operationKey()+"-")){a.setPlanDraftId(d.getId());audits.save(a);}
  return receipt;
 }
 private void apply(Command c,List<ActionCommand> actions){var r=new ActionBatchRequest();r.setUserId(TeamIdentity.user());r.setBaseRevision(c.baseRevision());r.setAuthorization("USER_CONFIRMED_SELF");r.setActions(actions);executor.apply(r);}
 static String text(JsonNode p,String key){if(!p.hasNonNull(key)||p.path(key).asText().isBlank())throw new IllegalArgumentException("缺少字段: "+key);if(key.equals("title")&&p.path(key).asText().length()>100)throw new IllegalArgumentException("任务名称最多100字");return p.path(key).asText();}
 static Long id(JsonNode p,String key){try{return Long.valueOf(text(p,key));}catch(NumberFormatException e){throw new IllegalArgumentException("ID 格式无效");}}
 static int integer(JsonNode p,String key,int min,int max){var n=p.path(key);if(!n.isIntegralNumber()||!n.canConvertToInt()||n.asInt()<min||n.asInt()>max)throw new IllegalArgumentException(key+" 超出范围");return n.asInt();}
}
