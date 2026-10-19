package com.studyplanner.business;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.contract.*;
import com.studyplanner.persistence.*;
import com.studyplanner.profile.ProfileService;
@Service
public class ScheduleStore {
    public static final String USER = "demo";
    final ScheduleRepository schedules;
    final TaskRepository tasks;
    final BlockRepository blocks;
    final CourseRepository courses;
    final ProfileService profile;
    private final Clock clock;
    public ScheduleStore(ScheduleRepository schedules, TaskRepository tasks, BlockRepository blocks,
            CourseRepository courses, ProfileService profile, Clock clock) {
        this.schedules=schedules; this.tasks=tasks; this.blocks=blocks; this.courses=courses; this.profile=profile; this.clock=clock;
    }
    public ScheduleEntity lock() { return schedules.lockById(USER).orElseThrow(); }
    public BlockRepository blocks() { return blocks; }
    public CourseRepository courses() { return courses; }
    public ProfileService profile() { return profile; }
    @Transactional(readOnly=true)
    public ScheduleSnapshot snapshot() {
        ScheduleEntity schedule = schedules.findById(USER).orElseThrow();
        List<TimeBlock> allBlocks = blocks.findByUserIdOrderById(USER).stream().map(ScheduleStore::block).toList();
        List<TaskSnapshot> allTasks = tasks.findByUserIdOrderById(USER).stream().map(t -> new TaskSnapshot(
                t.id,t.courseId,t.title,t.deadline,t.durationMinutes,t.priority,TaskStatus.valueOf(t.status),
                allBlocks.stream().filter(b -> b.kind()==TimeBlockKind.TASK && b.ownerId().equals(t.id)).toList())).toList();
        List<CourseSnapshot> allCourses = courses.findByUserIdOrderById(USER).stream().map(c -> new CourseSnapshot(
                c.id,c.name,allBlocks.stream().filter(b -> b.kind()==TimeBlockKind.COURSE && b.ownerId().equals(c.id)).toList())).toList();
        return new ScheduleSnapshot(1,clock.instant(),schedule.timezone,allCourses,allTasks,
                allBlocks.stream().filter(b -> b.kind()==TimeBlockKind.UNAVAILABLE || b.kind()==TimeBlockKind.PERSONAL).toList(),
                profile.activeFacts(),schedule.revision);
    }
    public void replaceTasks(List<TaskSnapshot> state) {
        List<TaskEntity> oldTasks = tasks.findByUserIdOrderById(USER);
        Set<String> ids = new HashSet<>(); state.forEach(t -> ids.add(t.id()));
        oldTasks.stream().filter(t -> !ids.contains(t.id)).forEach(tasks::delete);
        blocks.findByUserIdOrderById(USER).stream().filter(b -> b.kind.equals("TASK")).forEach(blocks::delete);
        blocks.flush();
        for (TaskSnapshot t : state) {
            TaskEntity e = new TaskEntity(); e.id=t.id(); e.userId=USER; e.courseId=t.courseId(); e.title=t.title();
            e.deadline=t.deadline(); e.durationMinutes=t.estimatedDurationMinutes(); e.priority=t.priority(); e.status=t.status().name(); tasks.save(e);
            t.scheduledBlocks().forEach(b -> blocks.save(entity(b)));
        }
    }
    public static TimeBlock block(BlockEntity e) {
        return new TimeBlock(e.id,e.ownerId,e.start,e.end,e.locked,TimeBlockKind.valueOf(e.kind));
    }
    public static BlockEntity entity(TimeBlock b) {
        BlockEntity e = new BlockEntity(); e.id=b.id(); e.userId=USER; e.ownerId=b.ownerId(); e.start=b.start();
        e.end=b.end(); e.locked=b.locked(); e.kind=b.kind().name(); return e;
    }
    public static ScheduleSnapshot withTasks(ScheduleSnapshot s, List<TaskSnapshot> tasks) {
        return new ScheduleSnapshot(s.schemaVersion(),s.now(),s.timezone(),s.courses(),tasks,s.lockedBlocks(),s.activeFacts(),s.planningRevision());
    }
}
