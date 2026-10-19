package com.studyplanner.team;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;
import com.studyplanner.business.*;
import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.TimeBlockKind;
import com.studyplanner.planning.*;
import com.studyplanner.executor.ActionCommand;
import com.studyplanner.enums.BlockStatus;
@Component public class TeamConstraintProjection {
 private final TeamSnapshotService snapshots;private final TimeBlockRepository blocks;private final HardConstraintValidator validator;private final TeamJson json;private final TaskStepRepository steps;
 public TeamConstraintProjection(TeamSnapshotService snapshots,TimeBlockRepository blocks,HardConstraintValidator validator,TeamJson json,TaskStepRepository steps){this.snapshots=snapshots;this.blocks=blocks;this.validator=validator;this.json=json;this.steps=steps;}
 private TaskSnapshot copy(TaskSnapshot t,String title,Instant deadline,int minutes,com.studyplanner.contract.TaskStatus status,List<com.studyplanner.contract.TimeBlock> bs){return new TaskSnapshot(t.id(),t.courseId(),title,deadline,minutes,t.priority(),status,bs);}
 private void change(List<TaskSnapshot> tasks,String id,UnaryOperator<TaskSnapshot> f){for(int i=0;i<tasks.size();i++)if(tasks.get(i).id().equals(id)){tasks.set(i,f.apply(tasks.get(i)));return;}throw new IllegalArgumentException("任务不存在");}
 private void removeBlock(List<TaskSnapshot> tasks,String id){for(int i=0;i<tasks.size();i++){var t=tasks.get(i);tasks.set(i,copy(t,t.title(),t.deadline(),t.estimatedDurationMinutes(),t.status(),t.scheduledBlocks().stream().filter(b -> !b.id().equals(id)).toList()));}}
 public List<String> validate(Long user,List<ActionCommand> commands){
  var base=snapshots.snapshot(user);var tasks=new ArrayList<>(base.tasks());
  try {
   for(var a:commands)switch(a.getType()){
    case CREATE_TASK -> {
     if(a.getDeadline()==null)throw new IllegalArgumentException("创建任务必须提供截止时间");
     if(!TeamSnapshotService.instant(a.getDeadline()).isAfter(base.now()))throw new IllegalArgumentException("截止时间必须在未来");
     int minutes=a.getRemainingMin()==null?0:a.getRemainingMin();if(minutes<=0||minutes>10080)throw new IllegalArgumentException("预计耗时应为1至10080分钟");
     String id="new-"+a.getOperationKey();var bs=new ArrayList<com.studyplanner.contract.TimeBlock>();
     if(a.getStartAt()!=null||a.getEndAt()!=null){if(a.getStartAt()==null||a.getEndAt()==null)throw new IllegalArgumentException("开始与结束时间必须成对提供");
      if(Duration.between(a.getStartAt(),a.getEndAt()).toMinutes()!=minutes)throw new IllegalArgumentException("指定时段应与耗时一致");
      bs.add(new com.studyplanner.contract.TimeBlock("new-block-"+a.getOperationKey(),id,TeamSnapshotService.instant(a.getStartAt()),TeamSnapshotService.instant(a.getEndAt()),false,TimeBlockKind.TASK));}
     tasks.add(new TaskSnapshot(id,a.getCourseId()==null?null:a.getCourseId().toString(),a.getTitle(),TeamSnapshotService.instant(a.getDeadline()),minutes,a.getPriority()==null?2:a.getPriority(),com.studyplanner.contract.TaskStatus.TODO,bs));
    }
    case UPDATE_TASK -> change(tasks,a.getTargetId().toString(),t -> {
     var status=a.getStatus()==null?t.status():com.studyplanner.contract.TaskStatus.valueOf(a.getStatus().name());
     boolean terminal=status==com.studyplanner.contract.TaskStatus.DONE||status==com.studyplanner.contract.TaskStatus.CANCELLED;
     if(!terminal&&a.getRemainingMin()!=null&&a.getRemainingMin()<=0)throw new IllegalArgumentException("未完成任务剩余耗时必须大于0");
     if(a.getDeadline()!=null&&!TeamSnapshotService.instant(a.getDeadline()).isAfter(base.now()))throw new IllegalArgumentException("新截止时间必须在未来");
     return copy(t,a.getTitle()==null?t.title():a.getTitle(),a.getDeadline()==null?t.deadline():TeamSnapshotService.instant(a.getDeadline()),a.getRemainingMin()==null||a.getRemainingMin()<=0?t.estimatedDurationMinutes():a.getRemainingMin(),status,terminal?List.of():t.scheduledBlocks());
    });
    case CREATE_BLOCK -> change(tasks,a.getTaskId().toString(),t -> {var bs=new ArrayList<>(t.scheduledBlocks());bs.add(new com.studyplanner.contract.TimeBlock("new-"+a.getOperationKey(),t.id(),TeamSnapshotService.instant(a.getStartAt()),TeamSnapshotService.instant(a.getEndAt()),Boolean.TRUE.equals(a.getLocked()),TimeBlockKind.TASK));return copy(t,t.title(),t.deadline(),t.estimatedDurationMinutes(),t.status(),bs);});
    case UPDATE_BLOCK -> {
     var old=blocks.findById(a.getTargetId()).orElseThrow();removeBlock(tasks,old.getId().toString());
     var status=a.getBlockStatus()==null?old.getStatus():a.getBlockStatus();
     if(status==BlockStatus.PLANNED)change(tasks,old.getTaskId().toString(),t -> {var bs=new ArrayList<>(t.scheduledBlocks());bs.add(new com.studyplanner.contract.TimeBlock(old.getId().toString(),t.id(),TeamSnapshotService.instant(a.getStartAt()==null?old.getStartAt():a.getStartAt()),TeamSnapshotService.instant(a.getEndAt()==null?old.getEndAt():a.getEndAt()),a.getLocked()==null?Boolean.TRUE.equals(old.getLocked()):a.getLocked(),TimeBlockKind.TASK));return copy(t,t.title(),t.deadline(),t.estimatedDurationMinutes(),t.status(),bs);});
    }
    case DELETE_BLOCK -> removeBlock(tasks,a.getTargetId().toString());
    case DELETE_TASK, DELETE_STEP -> throw new IllegalArgumentException("为保留关联与撤销历史，请使用取消操作，暂不支持硬删除任务或步骤");
    case CREATE_STEP, UPDATE_STEP -> {}
   }
   return validateState(base,tasks);
  }catch(IllegalArgumentException|DateTimeException e){return List.of(Objects.toString(e.getMessage(),"动作格式错误"));}
 }
 public List<String> validateState(ScheduleSnapshot base,List<TaskSnapshot> tasks){
  var issues=new ArrayList<String>();var old=new HashSet<com.studyplanner.contract.TimeBlock>();base.tasks().forEach(t -> old.addAll(t.scheduledBlocks()));
  var candidate=TeamSnapshotService.withTasks(base,tasks);
  for(var task:tasks){
   if(task.scheduledBlocks().stream().mapToLong(b -> Duration.between(b.start(),b.end()).toMinutes()).sum()>task.estimatedDurationMinutes())issues.add("安排总时长超过任务剩余耗时");
   for(var block:task.scheduledBlocks()){
    if(!old.contains(block)&&(block.start().isBefore(base.now())||block.start().getEpochSecond()%1800!=0||block.end().getEpochSecond()%1800!=0||block.start().getNano()!=0||block.end().getNano()!=0))issues.add("新安排必须在未来并对齐30分钟");
    var without=tasks.stream().map(t -> copy(t,t.title(),t.deadline(),t.estimatedDurationMinutes(),t.status(),t.scheduledBlocks().stream().filter(b -> !b.id().equals(block.id())).toList())).toList();
    validator.validate(TeamSnapshotService.withTasks(candidate,without),new CandidateBlock(task.id(),block.start(),block.end())).forEach(v -> issues.add(v.message()));
   }
  }
  return issues.stream().distinct().toList();
 }
 public void validateUndo(Long user,List<AgentAction> actions){
  var removedBlocks=new HashSet<Long>();var removedSteps=new HashSet<Long>();
  for(var a:actions){if(a.getType()==com.studyplanner.enums.ActionType.CREATE_BLOCK)removedBlocks.add(json.read(a.getAfterJson(),TimeBlock.class).getId());if(a.getType()==com.studyplanner.enums.ActionType.CREATE_STEP)removedSteps.add(json.read(a.getAfterJson(),TaskStep.class).getId());}
  for(var a:actions)if(a.getType()==com.studyplanner.enums.ActionType.CREATE_TASK){var id=json.read(a.getAfterJson(),Task.class).getId();
   if(blocks.findByUserId(user).stream().anyMatch(b -> b.getTaskId().equals(id)&&!removedBlocks.contains(b.getId()))||steps.findByTaskId(id).stream().anyMatch(s -> !removedSteps.contains(s.getId())))throw new com.studyplanner.common.BusinessException(409,"任务已有后续步骤或日程，请先撤销后续操作");}
  var base=snapshots.snapshot(user);var tasks=new ArrayList<>(base.tasks());
  for(var a:actions)switch(a.getType()){
   case CREATE_TASK -> {var t=json.read(a.getAfterJson(),Task.class);tasks.removeIf(x -> x.id().equals(t.getId().toString()));}
   case UPDATE_TASK -> {var old=json.read(a.getBeforeJson(),Task.class);change(tasks,old.getId().toString(),t -> TeamSnapshotService.task(old,t.scheduledBlocks()));}
   case CREATE_BLOCK -> {var b=json.read(a.getAfterJson(),TimeBlock.class);removeBlock(tasks,b.getId().toString());}
   case UPDATE_BLOCK -> {var b=json.read(a.getBeforeJson(),TimeBlock.class);removeBlock(tasks,b.getId().toString());if(b.getStatus()==BlockStatus.PLANNED)change(tasks,b.getTaskId().toString(),t -> {var bs=new ArrayList<>(t.scheduledBlocks());bs.add(TeamSnapshotService.block(b));return copy(t,t.title(),t.deadline(),t.estimatedDurationMinutes(),t.status(),bs);});}
   case CREATE_STEP, UPDATE_STEP -> {}
   default -> throw new com.studyplanner.common.BusinessException(409,"旧删除动作尚不支持安全恢复");
  }
  var issues=validateState(base,tasks);if(!issues.isEmpty())throw new com.studyplanner.common.BusinessException(409,"撤销违反硬约束: "+String.join("；",issues));
 }
}
