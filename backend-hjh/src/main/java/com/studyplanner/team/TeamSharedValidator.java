package com.studyplanner.team;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import com.studyplanner.executor.*;
import com.studyplanner.business.*;
@Component @Primary public class TeamSharedValidator implements ActionConstraintValidator {
 private final DefaultConstraintValidator basic;private final TeamConstraintProjection projection;private final TimeBlockRepository blocks;private final TaskStepRepository steps;private final PlanDraftRepository drafts;private final ImportJobRepository imports;
 public TeamSharedValidator(DefaultConstraintValidator basic,TeamConstraintProjection projection,TimeBlockRepository blocks,TaskStepRepository steps,PlanDraftRepository drafts,ImportJobRepository imports){this.basic=basic;this.projection=projection;this.blocks=blocks;this.steps=steps;this.drafts=drafts;this.imports=imports;}
 @Override public List<String> validate(Long user,List<ActionCommand> actions){
  var issues=new ArrayList<>(basic.validate(user,actions));if(!issues.isEmpty())return issues;
  for(var a:actions){
   if(a.getImportJobId()!=null && imports.findById(a.getImportJobId()).filter(job -> job.getUserId().equals(user)).isEmpty())issues.add("导入来源不属于当前用户");
   if(a.getType()==com.studyplanner.enums.ActionType.UPDATE_TASK && (a.getStatus()==com.studyplanner.enums.TaskStatus.DONE || a.getStatus()==com.studyplanner.enums.TaskStatus.CANCELLED)){
    for(var b:blocks.findByUserId(user))if(b.getTaskId().equals(a.getTargetId())&&b.getStatus()==com.studyplanner.enums.BlockStatus.PLANNED
      && actions.stream().noneMatch(c -> c.getType()==com.studyplanner.enums.ActionType.UPDATE_BLOCK && b.getId().equals(c.getTargetId()) && c.getBlockStatus()!=null && c.getBlockStatus()!=com.studyplanner.enums.BlockStatus.PLANNED))issues.add("结束任务时必须同时处理它的所有时间块");
   }
   if(a.getSourceDraftId()!=null&&drafts.findById(a.getSourceDraftId()).filter(d -> d.getUserId().equals(user)).isEmpty())issues.add("来源草案不属于当前用户");
   if(a.getType()==com.studyplanner.enums.ActionType.UPDATE_BLOCK&&a.getStepId()!=null){var block=blocks.findById(a.getTargetId()).orElseThrow();if(steps.findById(a.getStepId()).filter(s -> s.getTaskId().equals(block.getTaskId())).isEmpty())issues.add("步骤不属于该时间块的任务");}
  }
  if(!issues.isEmpty())return issues;
  return projection.validate(user,actions);
 }
}
