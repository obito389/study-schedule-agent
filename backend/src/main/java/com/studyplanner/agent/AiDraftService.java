package com.studyplanner.agent;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyplanner.business.*;
import com.studyplanner.common.*;
import com.studyplanner.contract.*;
import com.studyplanner.persistence.*;
import com.studyplanner.planning.*;

@Service
public class AiDraftService {
    private final ScheduleStore store;
    private final ActionExecutor executor;
    private final TaskMutations mutations;
    private final GreedyScheduler scheduler;
    private final AiDraftRepository drafts;
    private final PreviewRepository previews;
    private final JsonCodec json;
    private final ObjectMapper mapper;
    private final Clock clock;
    public AiDraftService(ScheduleStore store,ActionExecutor executor,TaskMutations mutations,GreedyScheduler scheduler,
            AiDraftRepository drafts,PreviewRepository previews,JsonCodec json,ObjectMapper mapper,Clock clock) {
        this.store=store;this.executor=executor;this.mutations=mutations;this.scheduler=scheduler;
        this.drafts=drafts;this.previews=previews;this.json=json;this.mapper=mapper;this.clock=clock;
    }
    @Transactional
    public AiPreview prepare(ModelAdapter.ModelResponse response,long baseRevision,String model,long elapsedMillis) {
        long revision=store.lock().revision;
        if(revision!=baseRevision) throw new ApiException(409,"REVISION_CONFLICT","AI 理解期间日程已更新，请重新生成");
        ScheduleSnapshot current=store.snapshot();String id=UUID.randomUUID().toString();
        if(!response.ambiguities().isEmpty()) return new AiPreview(id,"NEEDS_CLARIFICATION","DeepSeek",model,revision,
                elapsedMillis,response.reasonFacts(),response.ambiguities(),null,null);
        if(response.type()==null || !Set.of(ActionType.CREATE_TASK,ActionType.GENERATE_SCHEDULE,ActionType.QUERY_SCHEDULE).contains(response.type()))
            throw new ApiException(422,"AI_UNSUPPORTED_ACTION","当前 AI 支持创建单个任务、安排已有任务、查询日程，请缩小要求后重试");
        ScheduleSnapshot modelContext=current;
        if(response.type()==ActionType.GENERATE_SCHEDULE) modelContext=windowContext(current,response);
        var result=new AgentToolRegistry(scheduler).execute("ai-"+id,modelContext,response);
        if(result.status()==ActionStatus.NEEDS_CLARIFICATION)
            return new AiPreview(id,"NEEDS_CLARIFICATION","DeepSeek",model,revision,elapsedMillis,
                    response.reasonFacts(),List.of(result.message()),result.action(),null);
        if(response.type()==ActionType.QUERY_SCHEDULE)
            return new AiPreview(id,"ANSWER","DeepSeek",model,revision,elapsedMillis,response.reasonFacts(),List.of(),result.action(),null);
        ActionCommand command;
        ScheduleCandidate candidate=result.scheduleCandidate();
        if(response.type()==ActionType.CREATE_TASK) {
            ObjectNode payload=mapper.createObjectNode();
            for(String field:List.of("title","deadline","durationMinutes","priority","courseId","start","end")) {
                JsonNode value=response.changes().get(field);if(value!=null)payload.set(field,value);
            }
            if(!payload.path("durationMinutes").isIntegralNumber()) throw new ApiException(422,"AI_INVALID_TASK","AI 建议的耗时必须为整数，请补充要求");
            if(!payload.has("priority"))payload.put("priority",3);
            command=new ActionCommand("ai-"+id,revision,"CREATE_TASK","schedule:write:self",payload);
            List<TaskSnapshot> after;
            try {after=mutations.apply(command,current,List.of());}
            catch(java.time.DateTimeException e){throw new ApiException(422,"AI_INVALID_TASK","AI 返回的日期格式无效，请重新生成");}
            executor.validate(current,after);
            Set<String> oldIds=new HashSet<>();current.tasks().forEach(t -> oldIds.add(t.id()));
            candidate=new ScheduleCandidate(after.stream().filter(t -> !oldIds.contains(t.id())).flatMap(t ->
                    t.scheduledBlocks().stream().map(b -> new CandidateBlock(t.id(),b.start(),b.end()))).toList(),List.of());
        } else {
            if(candidate.blocks().isEmpty()) return new AiPreview(id,"NO_CAPACITY","DeepSeek",model,revision,elapsedMillis,
                    response.reasonFacts(),List.of("此要求下没有可新增的完整安排，请调整时间窗口、任务截止或耗时。"),result.action(),candidate);
            PreviewEntity p=new PreviewEntity();p.id=id;p.userId=ScheduleStore.USER;p.baseRevision=revision;
            p.candidateJson=json.write(candidate);p.createdAt=clock.instant();previews.save(p);
            command=new ActionCommand("ai-"+id,revision,"APPLY_PLAN","schedule:write:self",mapper.createObjectNode().put("previewId",id));
            executor.validate(current,mutations.apply(command,current,candidate.blocks()));
        }
        ActionSpec generated=result.action();
        ActionSpec pending=new ActionSpec(generated.schemaVersion(),generated.operationKey(),generated.baseRevision(),
                generated.type(),generated.targets(),generated.changes(),generated.authorizationScope(),generated.reasonFacts(),
                ActionStatus.NEEDS_CONFIRMATION,generated.errors());
        AiPreview preview=new AiPreview(id,"NEEDS_CONFIRMATION","DeepSeek",model,revision,elapsedMillis,
                response.reasonFacts(),List.of(),pending,candidate);
        AiDraftEntity draft=new AiDraftEntity();draft.id=id;draft.userId=ScheduleStore.USER;draft.baseRevision=revision;
        draft.previewJson=json.write(preview);draft.commandJson=json.write(command);draft.createdAt=clock.instant();drafts.save(draft);
        return preview;
    }
    @Transactional
    public ExecutionReceipt confirm(String id) {
        store.lock();
        AiDraftEntity draft=drafts.findById(id).filter(d -> d.userId.equals(ScheduleStore.USER))
                .orElseThrow(() -> new ApiException(404,"AI_DRAFT_NOT_FOUND","AI 草案不存在，请重新生成"));
        return executor.execute(json.read(draft.commandJson,ActionCommand.class));
    }
    private ScheduleSnapshot windowContext(ScheduleSnapshot s,ModelAdapter.ModelResponse response) {
        var changes=response.changes();boolean hasStart=changes.containsKey("start"),hasEnd=changes.containsKey("end");
        if(hasStart!=hasEnd)throw new ApiException(422,"AI_INVALID_WINDOW","请同时说明开始和结束时间");
        Instant start=s.now(),end=null;
        if(hasStart) {
            try {start=Instant.parse(changes.get("start").asText());end=Instant.parse(changes.get("end").asText());}
            catch(RuntimeException e){throw new ApiException(422,"AI_INVALID_WINDOW","AI 返回的时间窗口无效，请重新描述");}
            if(start.isBefore(s.now()) || !end.isAfter(start) || Duration.between(start,end).compareTo(Duration.ofDays(14))>0
                    || start.getEpochSecond()%1800!=0 || end.getEpochSecond()%1800!=0 || start.getNano()!=0 || end.getNano()!=0)
                throw new ApiException(422,"AI_INVALID_WINDOW","安排窗口必须在未来、对齐半小时且不超过 14 天");
        }
        Set<String> targets=new HashSet<>(response.targets());
        Set<String> known=new HashSet<>();s.tasks().forEach(t -> known.add(t.id()));
        if(!known.containsAll(targets))throw new ApiException(422,"AI_UNKNOWN_TASK","AI 引用了不存在的任务，请重新生成");
        Instant limit=end;
        List<TaskSnapshot> tasks=s.tasks().stream().map(t -> {
            boolean chosen=targets.isEmpty() || targets.contains(t.id());
            Instant deadline=chosen && limit!=null && limit.isBefore(t.deadline())?limit:t.deadline();
            return new TaskSnapshot(t.id(),t.courseId(),t.title(),deadline,t.estimatedDurationMinutes(),t.priority(),
                    chosen?t.status():TaskStatus.DONE,t.scheduledBlocks());
        }).toList();
        return new ScheduleSnapshot(s.schemaVersion(),start,s.timezone(),s.courses(),tasks,s.lockedBlocks(),s.activeFacts(),s.planningRevision());
    }
}
