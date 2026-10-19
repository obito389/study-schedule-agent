package com.studyplanner.business;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.studyplanner.common.ApiException;
import com.studyplanner.contract.*;
import com.studyplanner.persistence.CourseEntity;
import com.studyplanner.planning.HardConstraintValidator;

/** Prepare and validate in memory; persistence runs only after the executor accepts the change. */
@Component
public class CalendarMutations {
    public record Change(ScheduleSnapshot candidate, Runnable persist) {}
    private final ScheduleStore store;
    private final HardConstraintValidator validator;
    private final Clock clock;
    public CalendarMutations(ScheduleStore store, HardConstraintValidator validator, Clock clock) {
        this.store=store;this.validator=validator;this.clock=clock;
    }
    public boolean supports(String type) {
        return Set.of("CREATE_COURSE","DELETE_COURSE","CREATE_FIXED_BLOCK","DELETE_FIXED_BLOCK","SET_PREFERENCE").contains(type);
    }
    public Change prepare(ActionCommand command,ScheduleSnapshot base) {
        JsonNode p=command.payload();
        var courses=new ArrayList<>(base.courses());
        var fixed=new ArrayList<>(base.lockedBlocks());
        var facts=new ArrayList<>(base.activeFacts());
        Runnable write;
        switch(command.type()) {
            case "CREATE_COURSE", "CREATE_FIXED_BLOCK" -> {
                boolean course=command.type().equals("CREATE_COURSE");
                String title=TaskMutations.text(p,course?"name":"title").trim();
                if(title.length()>(course?200:80))throw new IllegalArgumentException("名称过长");
                String id=UUID.nameUUIDFromBytes(("calendar:"+command.operationKey()).getBytes(StandardCharsets.UTF_8)).toString();
                Instant start=Instant.parse(TaskMutations.text(p,"start")),end=Instant.parse(TaskMutations.text(p,"end"));
                TimeBlock block=new TimeBlock(id,course?id:title,start,end,true,
                        course?TimeBlockKind.COURSE:(p.path("unavailable").asBoolean()?TimeBlockKind.UNAVAILABLE:TimeBlockKind.PERSONAL));
                if(start.isBefore(clock.instant()) || start.getEpochSecond()%1800!=0 || end.getEpochSecond()%1800!=0 || start.getNano()!=0 || end.getNano()!=0)
                    throw new IllegalArgumentException("时段必须在未来并对齐 30 分钟");
                var violations=validator.validateFixedBlock(base,block);
                if(!violations.isEmpty())throw new ApiException(422,"TIME_OVERLAP","该时段与已有安排冲突");
                if(course) {
                    courses.add(new CourseSnapshot(id,title,List.of(block)));
                    write=() -> {var entity=new CourseEntity();entity.id=id;entity.userId=ScheduleStore.USER;entity.name=title;
                        store.courses().save(entity);store.blocks().save(ScheduleStore.entity(block));};
                } else { fixed.add(block);write=() -> store.blocks().save(ScheduleStore.entity(block)); }
            }
            case "DELETE_COURSE" -> {
                String id=TaskMutations.text(p,"courseId");
                var course=courses.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow(() -> new ApiException(404,"TARGET_NOT_FOUND","课程不存在"));
                if(base.tasks().stream().anyMatch(t -> id.equals(t.courseId())))throw new ApiException(409,"COURSE_IN_USE","课程已关联任务，需先修改任务所属课程");
                courses.remove(course);
                write=() -> {course.meetingBlocks().forEach(b -> store.blocks().deleteById(b.id()));store.courses().deleteById(id);};
            }
            case "DELETE_FIXED_BLOCK" -> {
                String id=TaskMutations.text(p,"blockId");
                var block=fixed.stream().filter(b -> b.id().equals(id)).findFirst().orElseThrow(() -> new ApiException(404,"TARGET_NOT_FOUND","固定时段不存在，请从对应入口修改"));
                fixed.remove(block);write=() -> store.blocks().deleteById(id);
            }
            case "SET_PREFERENCE" -> {
                String key=TaskMutations.text(p,"key"),action=TaskMutations.text(p,"action");
                boolean daily="planning.dailyLimitMinutes".equals(key);
                if(!daily && !(key.startsWith("duration.sample.") && !"SET".equals(action)))throw new IllegalArgumentException("当前支持每日学习上限及耗时证据的否认或删除");
                facts.removeIf(f -> key.equals(f.key()) && "GLOBAL".equals(f.scope()));
                switch(action) {
                    case "SET" -> {
                        JsonNode value=p.get("value");
                        if(value==null || !value.isIntegralNumber() || !value.canConvertToInt() || value.asInt()<30 || value.asInt()>1440)
                            throw new IllegalArgumentException("每日上限必须为 30 至 1440 分钟的整数");
                        facts.add(new ProfileFact(1,key,value,"GLOBAL",1,"USER_EXPLICIT",ProfileFactState.ACTIVE,List.of("manual"),store.profile().profileVersion()+1));
                        write=() -> store.profile().setExplicitPreference(key,value,"GLOBAL",List.of("manual"));
                    }
                    case "DELETE", "DENY" -> {
                        if(store.profile().history().stream().noneMatch(f -> key.equals(f.key()) && "GLOBAL".equals(f.scope())))throw new IllegalArgumentException("偏好不存在");
                        write=() -> {if(action.equals("DELETE"))store.profile().deleteFact(key,"GLOBAL",List.of("manual"));
                            else store.profile().denyFact(key,"GLOBAL",List.of("manual"));};
                    }
                    default -> throw new IllegalArgumentException("偏好操作无效");
                }
            }
            default -> throw new IllegalArgumentException("不支持的日历动作");
        }
        return new Change(new ScheduleSnapshot(base.schemaVersion(),base.now(),base.timezone(),courses,base.tasks(),fixed,facts,base.planningRevision()),write);
    }
}
