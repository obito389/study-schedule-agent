package com.studyplanner.team;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.business.*;
import com.studyplanner.executor.*;
import com.studyplanner.common.BusinessException;
import com.studyplanner.profile.ProfileService;
@Service public class TeamTaskService {
 private final ActionExecutor executor;private final AppUserRepository users;private final TaskRepository tasks;private final TimeBlockRepository blocks;private final ExecutionLogRepository logs;private final ProfileService profile;private final TeamSnapshotService snapshots;private final TeamJson json;private final LearningStatRepository statistics;
 public TeamTaskService(ActionExecutor executor,AppUserRepository users,TaskRepository tasks,TimeBlockRepository blocks,ExecutionLogRepository logs,ProfileService profile,TeamSnapshotService snapshots,TeamJson json,LearningStatRepository statistics){this.executor=executor;this.users=users;this.tasks=tasks;this.blocks=blocks;this.logs=logs;this.profile=profile;this.snapshots=snapshots;this.json=json;this.statistics=statistics;}
 private ActionBatchRequest batch(Long revision,List<ActionCommand> actions){var r=new ActionBatchRequest();r.setUserId(TeamIdentity.user());r.setBaseRevision(revision);r.setAuthorization("USER_CONFIRMED_SELF");r.setActions(actions);return r;}
 @Transactional public Object save(Long id,TeamTaskController.Input input){
  var c=TeamActionService.command(input.operationKey(),id==null?com.studyplanner.enums.ActionType.CREATE_TASK:com.studyplanner.enums.ActionType.UPDATE_TASK);c.setTargetId(id);c.setTitle(input.title());c.setDeadline(TeamSnapshotService.local(input.deadline()));c.setRemainingMin(input.durationMinutes());c.setPriority(input.priority());c.setCourseId(input.courseId());if(id==null)c.setInitialEstimateMin(input.durationMinutes());
  executor.apply(batch(input.baseRevision(),List.of(c)));return snapshots.snapshot();
 }
 @Transactional public Object finish(Long id,TeamTaskController.Finish input,boolean cancel){
  Long user=TeamIdentity.user();users.findByIdForUpdate(user).orElseThrow();
  var task=tasks.findById(id).filter(t -> t.getUserId().equals(user)).orElseThrow(() -> new BusinessException(404,"任务不存在"));
  if(!cancel){var previous=logs.findByUserId(user).stream().filter(e -> e.getFeedbackKey().equals(input.operationKey())).findFirst().orElse(null);
   if(previous!=null){if(!previous.getTaskId().equals(id)||previous.getActualMin()!=input.actualMinutes())throw new BusinessException(409,"反馈键已经绑定其他内容");return snapshots.snapshot();}}
  if(task.getStatus()==com.studyplanner.enums.TaskStatus.DONE||task.getStatus()==com.studyplanner.enums.TaskStatus.CANCELLED)throw new BusinessException(409,"任务已经结束");
  var commands=new ArrayList<ActionCommand>();var c=TeamActionService.command(input.operationKey(),com.studyplanner.enums.ActionType.UPDATE_TASK);c.setTargetId(id);c.setStatus(cancel?com.studyplanner.enums.TaskStatus.CANCELLED:com.studyplanner.enums.TaskStatus.DONE);if(!cancel)c.setRemainingMin(0);commands.add(c);
  int i=0;for(var block:blocks.findByUserId(user))if(block.getTaskId().equals(id)&&block.getStatus()==com.studyplanner.enums.BlockStatus.PLANNED){var b=TeamActionService.command(input.operationKey()+"-"+(i++),com.studyplanner.enums.ActionType.UPDATE_BLOCK);b.setTargetId(block.getId());b.setBlockStatus(cancel?com.studyplanner.enums.BlockStatus.CANCELLED:com.studyplanner.enums.BlockStatus.COMPLETED);commands.add(b);}
  int estimate=task.getInitialEstimateMin()!=null?task.getInitialEstimateMin():Math.max(1,task.getRemainingMin());executor.apply(batch(input.baseRevision(),commands));
  if(!cancel){var log=new ExecutionLog();log.setUserId(user);log.setTaskId(id);log.setActualMin(input.actualMinutes());log.setRemainingAfter(0);log.setFeedbackKey(input.operationKey());logs.save(log);profile.recordDurationSample("GLOBAL",estimate,input.actualMinutes(),input.operationKey());
    var estimateResult=profile.estimateDuration("GLOBAL",estimate,false);var stat=statistics.findByUserId(user).stream().filter(s -> "GLOBAL".equals(s.getTaskType())).findFirst().orElseGet(LearningStat::new);
    stat.setUserId(user);stat.setTaskType("GLOBAL");stat.setSampleCount(estimateResult.sampleCount());stat.setRatio(java.math.BigDecimal.valueOf(estimateResult.multiplier()));statistics.save(stat);}
  return snapshots.snapshot();
 }
 @Transactional public Object addStep(Long id,TeamTaskController.Step input){
  var c=TeamActionService.command(input.operationKey(),com.studyplanner.enums.ActionType.CREATE_STEP);c.setTaskId(id);c.setOrderNo(input.orderNo());c.setStepTitle(input.title());c.setStepRemainingMin(input.minutes());executor.apply(batch(input.baseRevision(),List.of(c)));return snapshots.snapshot();
 }
}
