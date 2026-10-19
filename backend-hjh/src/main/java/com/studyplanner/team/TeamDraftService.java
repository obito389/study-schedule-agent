package com.studyplanner.team;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.business.*;
import com.studyplanner.executor.*;
import com.studyplanner.undo.*;
import com.studyplanner.contract.ActionSpec;
import com.studyplanner.contract.ActionStatus;
import com.studyplanner.contract.ActionType;
import com.studyplanner.common.BusinessException;
@Service public class TeamDraftService {
 public record Bundle(ActionSpec action,ActionBatchRequest batch,UndoResult undo,String undoKey,Long undoBase) {}
 private final PlanDraftRepository drafts;private final AppUserRepository users;private final ActionExecutor executor;private final UndoExecutor undo;private final TeamJson json;private final Clock clock;
 public TeamDraftService(PlanDraftRepository drafts,AppUserRepository users,ActionExecutor executor,UndoExecutor undo,TeamJson json,Clock clock){this.drafts=drafts;this.users=users;this.executor=executor;this.undo=undo;this.json=json;this.clock=clock;}
 public static ActionSpec status(ActionSpec a,ActionStatus status){return new ActionSpec(a.schemaVersion(),a.operationKey(),a.baseRevision(),a.type(),a.targets(),a.changes(),a.authorizationScope(),a.reasonFacts(),status,a.errors());}
 @Transactional public ActionSpec prepare(ActionSpec action,ActionBatchRequest batch){
  var user=users.findByIdForUpdate(TeamIdentity.user()).orElseThrow();
  if(user.getPlanningRevision()!=action.baseRevision())throw new BusinessException(409,"生成建议期间日程已更新，请重新预览");
  var d=new PlanDraft();d.setUserId(user.getId());d.setBaseRevision(action.baseRevision());d.setBaseProfileVersion(user.getProfileVersion());d.setRequestKey(action.operationKey());
  d.setRangeStart(TeamSnapshotService.local(clock.instant()));d.setRangeEnd(TeamSnapshotService.local(clock.instant().plus(Duration.ofDays(14))));
  drafts.saveAndFlush(d);batch.setPlanDraftId(d.getId());batch.setUserId(user.getId());batch.setBaseRevision(action.baseRevision());
  var preview=executor.preview(batch);if(!preview.isValid())throw new BusinessException(422,String.join("；",preview.getViolations()));
  var pending=status(action,ActionStatus.NEEDS_CONFIRMATION);d.setProposalJson(json.write(new Bundle(pending,batch,null,null,null)));drafts.save(d);return pending;
 }
 private PlanDraft owned(String key){return drafts.findByUserId(TeamIdentity.user()).stream().filter(d -> key.equals(d.getRequestKey())).findFirst().orElseThrow(() -> new BusinessException(404,"动作草案不存在"));}
 @Transactional public ActionSpec confirm(String key){
  users.findByIdForUpdate(TeamIdentity.user()).orElseThrow();var d=owned(key);
  if(d.getStatus()==com.studyplanner.enums.DraftStatus.CANCELLED)throw new BusinessException(409,"该操作已撤销，不可再次确认");
  var bundle=json.read(d.getProposalJson(),Bundle.class);executor.apply(bundle.batch());
  d.setStatus(com.studyplanner.enums.DraftStatus.APPLIED);d.setAppliedAt(TeamSnapshotService.local(clock.instant()));drafts.save(d);return status(bundle.action(),ActionStatus.APPLIED);
 }
 @Transactional public Map<String,Object> undo(String key,String undoKey,long revision){
  users.findByIdForUpdate(TeamIdentity.user()).orElseThrow();var d=owned(key);var bundle=json.read(d.getProposalJson(),Bundle.class);
  UndoResult result=bundle.undo();
  if(result!=null){if(!Objects.equals(bundle.undoKey(),undoKey)||!Objects.equals(bundle.undoBase(),revision))throw new BusinessException(409,"撤销键已绑定其他请求");}
  else {
   if(d.getStatus()!=com.studyplanner.enums.DraftStatus.APPLIED)throw new BusinessException(409,"动作尚未执行");
   var request=new UndoRequest();request.setUserId(TeamIdentity.user());request.setPlanDraftId(d.getId());request.setBaseRevision(revision);
   result=undo.undoBatch(request);d.setStatus(com.studyplanner.enums.DraftStatus.CANCELLED);
   d.setProposalJson(json.write(new Bundle(bundle.action(),bundle.batch(),result,undoKey,revision)));drafts.save(d);
  }
  return Map.of("httpStatus",200,"executionId",undoKey,"reversedExecutionId",key,"operationKey",undoKey,"actionStatus","APPLIED","planningRevision",result.getNewRevision());
 }
}
