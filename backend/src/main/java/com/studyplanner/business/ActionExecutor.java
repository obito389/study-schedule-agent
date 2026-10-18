package com.studyplanner.business;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.studyplanner.common.*;
import com.studyplanner.contract.*;
import com.studyplanner.persistence.*;
import com.studyplanner.planning.*;
@Service
public class ActionExecutor {
    private final ScheduleStore store;
    private final ExecutionRepository executions;
    private final PreviewRepository previews;
    private final TaskMutations mutations;
    private final HardConstraintValidator validator;
    private final JsonCodec json;
    private final Clock clock;
    private static final TypeReference<List<TaskSnapshot>> TASKS = new TypeReference<>() {};
    public ActionExecutor(ScheduleStore store, ExecutionRepository executions, PreviewRepository previews,
            TaskMutations mutations, HardConstraintValidator validator, JsonCodec json, Clock clock) {
        this.store=store;this.executions=executions;this.previews=previews;this.mutations=mutations;
        this.validator=validator;this.json=json;this.clock=clock;
    }
    @Transactional
    public ExecutionReceipt execute(ActionCommand command) {
        ScheduleEntity revision=store.lock();
        String request=json.write(command);
        ExecutionEntity existing=executions.findByUserIdAndOperationKey(ScheduleStore.USER,command.operationKey()).orElse(null);
        if (existing!=null) {
            if (!json.read(existing.requestJson,com.fasterxml.jackson.databind.JsonNode.class)
                    .equals(json.read(request,com.fasterxml.jackson.databind.JsonNode.class))) return failure(null,command,revision.revision,409,
                    "DUPLICATE_OPERATION_MISMATCH","幂等键已经绑定其他请求");
            return json.read(existing.receiptJson,ExecutionReceipt.class);
        }
        ScheduleSnapshot snapshot=store.snapshot();
        List<TaskSnapshot> before=snapshot.tasks(), after;
        ExecutionEntity original=null;
        String executionId=UUID.randomUUID().toString();
        try {
            if (!"schedule:write:self".equals(command.authorizationScope()))
                throw new ApiException(403,"AUTHORIZATION_DENIED","没有当前日程的写入权限");
            if (command.baseRevision()!=revision.revision)
                throw new ApiException(409,"REVISION_CONFLICT","日程已更新，请刷新并重新预览");
            if (command.type().equals("UNDO")) {
                original=executions.findById(TaskMutations.text(command.payload(),"executionId"))
                        .filter(e -> e.userId.equals(ScheduleStore.USER))
                        .orElseThrow(() -> new ApiException(404,"TARGET_NOT_FOUND","执行记录不存在"));
                ExecutionReceipt receipt=json.read(original.receiptJson,ExecutionReceipt.class);
                if (!receipt.undoable() || original.undone)
                    throw new ApiException(409,"UNDO_NOT_AVAILABLE","该动作不能撤销或已经撤销");
                if (!json.read(original.afterJson,TASKS).equals(before))
                    throw new ApiException(409,"UNDO_CONFLICT","任务已发生后续变化，不能安全撤销");
                after=json.read(original.beforeJson,TASKS);
            } else {
                List<CandidateBlock> blocks=List.of();
                if (command.type().equals("APPLY_PLAN")) {
                    PreviewEntity preview=previews.findById(TaskMutations.text(command.payload(),"previewId"))
                            .filter(p -> p.userId.equals(ScheduleStore.USER)).orElseThrow(
                                    () -> new ApiException(404,"TARGET_NOT_FOUND","预览不存在"));
                    if (preview.baseRevision!=revision.revision)
                        throw new ApiException(409,"REVISION_CONFLICT","预览已过期，请重新生成");
                    blocks=json.read(preview.candidateJson,ScheduleCandidate.class).blocks();
                }
                after=mutations.apply(command,snapshot,blocks);
            }
            validate(snapshot,after);
        } catch (ApiException e) {
            ExecutionReceipt receipt=failure(executionId,command,revision.revision,e.httpStatus,e.code,e.getMessage());
            save(command,request,receipt,before,before); return receipt;
        } catch (IllegalArgumentException e) {
            ExecutionReceipt receipt=failure(executionId,command,revision.revision,400,"INVALID_ACTION",e.getMessage());
            save(command,request,receipt,before,before); return receipt;
        }
        store.replaceTasks(after);
        if (command.type().equals("COMPLETE_TASK")) {
            String taskId=TaskMutations.text(command.payload(),"taskId");
            TaskSnapshot task=before.stream().filter(t -> t.id().equals(taskId)).findFirst().orElseThrow();
            store.profile().recordDurationSample("GLOBAL",task.estimatedDurationMinutes(),
                    command.payload().path("actualMinutes").asInt(),executionId);
        }
        if (original!=null) { original.undone=true; executions.save(original); }
        revision.revision++;
        boolean undoable=!command.type().equals("COMPLETE_TASK") && !command.type().equals("UNDO");
        ExecutionReceipt receipt=new ExecutionReceipt(executionId,command.operationKey(),command.type(),"APPLIED",
                revision.revision,200,"已保存",List.of(),undoable);
        save(command,request,receipt,before,after);
        return receipt;
    }
    public void validate(ScheduleSnapshot base,List<TaskSnapshot> tasks) {
        ScheduleSnapshot candidate=ScheduleStore.withTasks(base,tasks);
        Set<TimeBlock> previous=new HashSet<>(); base.tasks().forEach(t -> previous.addAll(t.scheduledBlocks()));
        Set<String> courseIds=new HashSet<>();base.courses().forEach(c -> courseIds.add(c.id()));
        for (TaskSnapshot task:tasks) {
            if (task.courseId()!=null && !courseIds.contains(task.courseId()))
                throw new ApiException(404,"TARGET_NOT_FOUND","所选课程不存在");
            for (TimeBlock block:task.scheduledBlocks()) {
                if (!previous.contains(block) && (block.start().isBefore(clock.instant())
                        || block.start().getEpochSecond()%1800!=0 || block.end().getEpochSecond()%1800!=0
                        || block.start().getNano()!=0 || block.end().getNano()!=0))
                    throw new ApiException(422,"INVALID_TIME_SLOT","新安排必须在未来且对齐 30 分钟时间槽");
                List<TaskSnapshot> without=tasks.stream().map(t -> TaskMutations.copy(t,t.status(),t.scheduledBlocks()
                        .stream().filter(b -> !b.id().equals(block.id())).toList())).toList();
                List<ConstraintViolation> violations=validator.validate(ScheduleStore.withTasks(candidate,without),
                        new CandidateBlock(task.id(),block.start(),block.end()));
                if (!violations.isEmpty()) {
                    ConstraintViolation v=violations.getFirst();
                    throw new ApiException(422,v.code().name(),v.message());
                }
            }
        }
    }
    @Transactional(readOnly=true)
    public List<ExecutionReceipt> history() {
        return executions.findByUserIdOrderById(ScheduleStore.USER).stream()
                .sorted(Comparator.comparing((ExecutionEntity e) -> e.createdAt).reversed())
                .map(e -> { ExecutionReceipt r=json.read(e.receiptJson,ExecutionReceipt.class);
                    return new ExecutionReceipt(r.executionId(),r.operationKey(),r.type(),r.status(),r.planningRevision(),
                            r.httpStatus(),r.message(),r.errors(),r.undoable()&&!e.undone); }).toList();
    }
    private ExecutionReceipt failure(String id,ActionCommand c,long rev,int http,String code,String message) {
        return new ExecutionReceipt(id,c.operationKey(),c.type(),"FAILED",rev,http,message,
                List.of(new ContractError(code,message,List.of())),false);
    }
    private void save(ActionCommand c,String request,ExecutionReceipt receipt,List<TaskSnapshot> before,List<TaskSnapshot> after) {
        ExecutionEntity e=new ExecutionEntity();e.id=receipt.executionId();e.userId=ScheduleStore.USER;
        e.operationKey=c.operationKey();e.requestJson=request;e.receiptJson=json.write(receipt);
        e.beforeJson=json.write(before);e.afterJson=json.write(after);e.createdAt=clock.instant();executions.save(e);
    }
}
