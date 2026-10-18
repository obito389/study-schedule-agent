package com.studyplanner.business;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.studyplanner.common.ApiException;
import com.studyplanner.contract.*;
import com.studyplanner.persistence.*;
@Service
public class CalendarService {
    private final ScheduleStore store;
    private final ActionExecutor executor;
    private final Clock clock;
    public CalendarService(ScheduleStore store,ActionExecutor executor,Clock clock) {
        this.store=store;this.executor=executor;this.clock=clock;
    }
    @Transactional
    public ScheduleSnapshot addCourse(String name,Instant start,Instant end,long baseRevision) {
        var revision=checkRevision(baseRevision);
        if (name==null || name.isBlank() || name.length()>200) throw new IllegalArgumentException("课程名应为 1 至 200 字符");
        String id=UUID.randomUUID().toString();
        TimeBlock block=new TimeBlock(UUID.randomUUID().toString(),id,start,end,true,TimeBlockKind.COURSE);
        validateNewBlock(block);
        CourseEntity course=new CourseEntity();course.id=id;course.userId=ScheduleStore.USER;course.name=name.trim();
        store.courses().save(course);store.blocks().save(ScheduleStore.entity(block));revision.revision++;
        return store.snapshot();
    }
    @Transactional
    public ScheduleSnapshot addBlock(String title,Instant start,Instant end,boolean unavailable,long baseRevision) {
        var revision=checkRevision(baseRevision);
        if (title==null || title.isBlank() || title.length()>80) throw new IllegalArgumentException("时段名称应为 1 至 80 字符");
        TimeBlock block=new TimeBlock(UUID.randomUUID().toString(),title,start,end,true,
                unavailable?TimeBlockKind.UNAVAILABLE:TimeBlockKind.PERSONAL);
        validateNewBlock(block);store.blocks().save(ScheduleStore.entity(block));revision.revision++;
        return store.snapshot();
    }
    @Transactional
    public ScheduleSnapshot deleteCourse(String id,long baseRevision) {
        var revision=checkRevision(baseRevision);
        CourseEntity course=store.courses().findById(id).filter(c -> c.userId.equals(ScheduleStore.USER))
                .orElseThrow(() -> new ApiException(404,"TARGET_NOT_FOUND","课程不存在"));
        if (store.snapshot().tasks().stream().anyMatch(t -> id.equals(t.courseId())))
            throw new ApiException(409,"COURSE_IN_USE","课程已关联任务，需先修改任务所属课程");
        store.blocks().findByUserIdOrderById(ScheduleStore.USER).stream().filter(b -> b.ownerId.equals(id)).forEach(store.blocks()::delete);
        store.courses().delete(course);revision.revision++;return store.snapshot();
    }
    @Transactional
    public ScheduleSnapshot deleteBlock(String id,long baseRevision) {
        var revision=checkRevision(baseRevision);
        BlockEntity b=store.blocks().findById(id).filter(e -> e.userId.equals(ScheduleStore.USER))
                .orElseThrow(() -> new ApiException(404,"TARGET_NOT_FOUND","时段不存在"));
        if (!b.kind.equals("PERSONAL") && !b.kind.equals("UNAVAILABLE"))
            throw new IllegalArgumentException("课程和任务时间块请从对应入口修改");
        store.blocks().delete(b);revision.revision++;return store.snapshot();
    }
    @Transactional
    public List<ProfileFact> preference(String key,JsonNode value,String action,long baseRevision) {
        var revision=checkRevision(baseRevision);
        boolean dailyLimit="planning.dailyLimitMinutes".equals(key);
        if (!dailyLimit && !(key!=null && key.startsWith("duration.sample.") && !"SET".equals(action)))
            throw new IllegalArgumentException("当前支持每日学习上限及耗时证据的否认或删除");
        switch(action) {
            case "SET" -> {
                if (value==null || !value.isIntegralNumber() || value.asInt()<30 || value.asInt()>1440)
                    throw new IllegalArgumentException("每日上限必须为 30 至 1440 分钟的整数");
                store.profile().setExplicitPreference(key,value,"GLOBAL",List.of("manual"));
            }
            case "DELETE" -> store.profile().deleteFact(key,"GLOBAL",List.of("manual"));
            case "DENY" -> store.profile().denyFact(key,"GLOBAL",List.of("manual"));
            default -> throw new IllegalArgumentException("偏好操作无效");
        }
        ScheduleSnapshot candidate=store.snapshot();executor.validate(candidate,candidate.tasks());
        revision.revision++;return store.profile().history();
    }
    private ScheduleEntity checkRevision(long revision) {
        var current=store.lock();
        if (current.revision!=revision) throw new ApiException(409,"REVISION_CONFLICT","日程已更新，请刷新");
        return current;
    }
    private void validateNewBlock(TimeBlock b) {
        if (b.start().isBefore(clock.instant()) || b.start().getEpochSecond()%1800!=0 || b.end().getEpochSecond()%1800!=0
                || b.start().getNano()!=0 || b.end().getNano()!=0)
            throw new IllegalArgumentException("时段必须在未来并对齐 30 分钟");
        boolean conflict=store.blocks().findByUserIdOrderById(ScheduleStore.USER).stream()
                .anyMatch(e -> b.start().isBefore(e.end) && e.start.isBefore(b.end()));
        if (conflict) throw new ApiException(422,"TIME_OVERLAP","该时段与已有安排冲突");
    }
}
