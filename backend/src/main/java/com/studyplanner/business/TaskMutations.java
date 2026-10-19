package com.studyplanner.business;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.studyplanner.common.ApiException;
import com.studyplanner.contract.*;
import com.studyplanner.planning.CandidateBlock;
@Component
public class TaskMutations {
    public List<TaskSnapshot> apply(ActionCommand command, ScheduleSnapshot snapshot, List<CandidateBlock> plan) {
        List<TaskSnapshot> tasks = new ArrayList<>(snapshot.tasks());
        JsonNode p = command.payload();
        if (command.type().equals("CREATE_TASK")) {
            String id = UUID.nameUUIDFromBytes(("demo:"+command.operationKey()).getBytes(StandardCharsets.UTF_8)).toString();
            TaskSnapshot task=taskInput(id,p,snapshot.now(),List.of());
            if(p.hasNonNull("start") || p.hasNonNull("end")) {
                Instant start=Instant.parse(text(p,"start")),end=Instant.parse(text(p,"end"));
                long seconds=Duration.between(start,end).getSeconds();
                if(seconds!=(long)task.estimatedDurationMinutes()*60)
                    throw new IllegalArgumentException("指定时间段必须与预计耗时一致");
                task=copy(task,task.status(),List.of(new TimeBlock(UUID.randomUUID().toString(),id,start,end,false,TimeBlockKind.TASK)));
            }
            tasks.add(task);
        } else if (command.type().equals("APPLY_PLAN")) {
            if (plan.isEmpty()) throw new ApiException(422,"EMPTY_PLAN","没有可保存的新时间块");
            for (CandidateBlock b : plan) {
                int i = index(tasks,b.taskId()); TaskSnapshot t = tasks.get(i);
                List<TimeBlock> blocks = new ArrayList<>(t.scheduledBlocks());
                blocks.add(new TimeBlock(UUID.randomUUID().toString(),t.id(),b.start(),b.end(),false,TimeBlockKind.TASK));
                tasks.set(i,copy(t,t.status(),blocks));
            }
        } else {
            int i = index(tasks,text(p,"taskId")); TaskSnapshot t = tasks.get(i);
            switch(command.type()) {
                case "UPDATE_TASK" -> {
                    if (t.status()==TaskStatus.DONE || t.status()==TaskStatus.CANCELLED)
                        throw new ApiException(409,"TASK_NOT_EDITABLE","已完成或取消的任务不能修改");
                    tasks.set(i,taskInput(t.id(),p,snapshot.now(),t.scheduledBlocks()));
                }
                case "CANCEL_TASK", "COMPLETE_TASK" -> {
                    if (t.status()==TaskStatus.DONE || t.status()==TaskStatus.CANCELLED)
                        throw new ApiException(409,"TASK_NOT_EDITABLE","任务已经结束");
                    if (t.scheduledBlocks().stream().anyMatch(TimeBlock::locked))
                        throw new ApiException(422,"LOCKED_BLOCK_OVERLAP","任务包含锁定时间块");
                    if (command.type().equals("COMPLETE_TASK")) {
                        int actual = p.path("actualMinutes").asInt();
                        if (actual<=0 || actual>10080) throw new IllegalArgumentException("实际耗时应为 1 至 10080 分钟");
                    }
                    tasks.set(i,copy(t,command.type().equals("COMPLETE_TASK")?TaskStatus.DONE:TaskStatus.CANCELLED,List.of()));
                }
                case "MOVE_TASK" -> {
                    if (t.status()==TaskStatus.DONE || t.status()==TaskStatus.CANCELLED)
                        throw new ApiException(409,"TASK_NOT_EDITABLE","任务已经结束");
                    String blockId = text(p,"blockId");
                    TimeBlock block = t.scheduledBlocks().stream().filter(b -> b.id().equals(blockId)).findFirst()
                            .orElseThrow(() -> new ApiException(404,"TARGET_NOT_FOUND","时间块不存在"));
                    if (block.locked()) throw new ApiException(422,"LOCKED_BLOCK_OVERLAP","锁定时间块不能移动");
                    Instant start = Instant.parse(text(p,"start")), end = Instant.parse(text(p,"end"));
                    if (Duration.between(start,end).toMinutes()!=Duration.between(block.start(),block.end()).toMinutes())
                        throw new IllegalArgumentException("移动时间块不能改变时长");
                    tasks.set(i,copy(t,t.status(),t.scheduledBlocks().stream().map(b -> b.id().equals(blockId)
                            ? new TimeBlock(b.id(),b.ownerId(),start,end,false,TimeBlockKind.TASK):b).toList()));
                }
                default -> throw new IllegalArgumentException("不支持的写动作");
            }
        }
        return tasks.stream().map(t -> copy(t,t.status(),t.scheduledBlocks().stream().sorted(Comparator.comparing(TimeBlock::id)).toList()))
                .sorted(Comparator.comparing(TaskSnapshot::id)).toList();
    }
    private static TaskSnapshot taskInput(String id, JsonNode p, Instant now, List<TimeBlock> blocks) {
        String title=text(p,"title").trim();
        if (title.length()>200) throw new IllegalArgumentException("标题不能超过 200 字符");
        Instant deadline=Instant.parse(text(p,"deadline"));
        if (!deadline.isAfter(now)) throw new IllegalArgumentException("截止时间必须晚于当前时间");
        int duration=p.path("durationMinutes").asInt();
        if (duration<1 || duration>10080) throw new IllegalArgumentException("预计耗时应为 1 至 10080 分钟");
        long scheduled=blocks.stream().mapToLong(b -> Duration.between(b.start(),b.end()).toMinutes()).sum();
        if (scheduled>duration) throw new ApiException(422,"DURATION_BELOW_SCHEDULED","预计耗时不能低于已经安排的时长，请先撤销排程");
        String courseId=p.path("courseId").asText(null);
        if (courseId!=null && courseId.isBlank()) courseId=null;
        return new TaskSnapshot(id,courseId,title,deadline,duration,p.path("priority").asInt(3),TaskStatus.TODO,blocks);
    }
    public static TaskSnapshot copy(TaskSnapshot t, TaskStatus status, List<TimeBlock> blocks) {
        return new TaskSnapshot(t.id(),t.courseId(),t.title(),t.deadline(),t.estimatedDurationMinutes(),t.priority(),status,blocks);
    }
    public static String text(JsonNode p,String field) {
        String value=p.path(field).asText("");
        if (value.isBlank()) throw new IllegalArgumentException("缺少字段: "+field);
        return value;
    }
    private static int index(List<TaskSnapshot> tasks,String id) {
        for (int i=0;i<tasks.size();i++) if (tasks.get(i).id().equals(id)) return i;
        throw new ApiException(404,"TARGET_NOT_FOUND","任务不存在");
    }
}
