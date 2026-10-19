package com.studyplanner.team;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import com.studyplanner.business.*;
import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.CourseSnapshot;
import com.studyplanner.contract.TimeBlockKind;
import com.studyplanner.profile.ProfileService;
import com.studyplanner.enums.BlockStatus;
@Service public class TeamSnapshotService {
 public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 private final AppUserRepository users;private final TaskRepository tasks;private final TimeBlockRepository blocks;
 private final CourseRepository courses;private final CourseSessionRepository sessions;private final SemesterRepository semesters;
 private final AvailabilityWindowRepository windows;private final ProfileService profile;private final Clock clock;
 public TeamSnapshotService(AppUserRepository users,TaskRepository tasks,TimeBlockRepository blocks,CourseRepository courses,
   CourseSessionRepository sessions,SemesterRepository semesters,AvailabilityWindowRepository windows,ProfileService profile,Clock clock){
  this.users=users;this.tasks=tasks;this.blocks=blocks;this.courses=courses;this.sessions=sessions;this.semesters=semesters;this.windows=windows;this.profile=profile;this.clock=clock;
 }
 public static Instant instant(LocalDateTime time){return time.atZone(ZONE).toInstant();}
 public static LocalDateTime local(Instant time){return LocalDateTime.ofInstant(time,ZONE);}
 public static TaskSnapshot task(Task t,List<com.studyplanner.contract.TimeBlock> blocks){
  int minutes=t.getRemainingMin()!=null&&t.getRemainingMin()>0?t.getRemainingMin():(t.getInitialEstimateMin()!=null&&t.getInitialEstimateMin()>0?t.getInitialEstimateMin():30);
  return new TaskSnapshot(t.getId().toString(),t.getCourseId()==null?null:t.getCourseId().toString(),t.getTitle(),
   t.getDeadline()==null?Instant.parse("2100-01-01T00:00:00Z"):instant(t.getDeadline()),minutes,t.getPriority(),
   com.studyplanner.contract.TaskStatus.valueOf(t.getStatus().name()),blocks);
 }
 public static com.studyplanner.contract.TimeBlock block(TimeBlock b){return new com.studyplanner.contract.TimeBlock(b.getId().toString(),b.getTaskId().toString(),instant(b.getStartAt()),instant(b.getEndAt()),Boolean.TRUE.equals(b.getLocked()),TimeBlockKind.TASK);}
 @Transactional(readOnly=true) public ScheduleSnapshot snapshot(){return snapshot(TeamIdentity.user());}
 @Transactional(readOnly=true) public ScheduleSnapshot snapshot(Long userId){
  var user=users.findById(userId).orElseThrow();var allBlocks=blocks.findByUserId(userId).stream().filter(b -> b.getStatus()==BlockStatus.PLANNED).toList();
  var ts=tasks.findByUserId(userId).stream().sorted(Comparator.comparing(Task::getId)).map(t -> task(t,
   t.getStatus()==com.studyplanner.enums.TaskStatus.DONE||t.getStatus()==com.studyplanner.enums.TaskStatus.CANCELLED?List.of():allBlocks.stream().filter(b -> b.getTaskId().equals(t.getId())).map(TeamSnapshotService::block).toList())).toList();
  LocalDate today=clock.instant().atZone(ZONE).toLocalDate();var cs=new ArrayList<CourseSnapshot>();
  for(var c:courses.findByUserId(userId,Pageable.unpaged()).getContent()){
   var semester=semesters.findById(c.getSemesterId()).orElseThrow();var meetings=new ArrayList<com.studyplanner.contract.TimeBlock>();
   for(var session:sessions.findByCourseId(c.getId()))for(LocalDate date=today.minusDays(7);date.isBefore(today.plusDays(35));date=date.plusDays(1)){
    long week=ChronoUnit.DAYS.between(semester.getStartDate(),date)/7+1;
    if(date.isBefore(semester.getStartDate())||week>semester.getWeekCount()||week<session.getStartWeek()||week>session.getEndWeek()||date.getDayOfWeek().getValue()!=session.getWeekday())continue;
    if(session.getWeekType()==com.studyplanner.enums.WeekType.ODD&&week%2==0||session.getWeekType()==com.studyplanner.enums.WeekType.EVEN&&week%2!=0)continue;
    meetings.add(new com.studyplanner.contract.TimeBlock("course-"+session.getId()+"-"+date,c.getId().toString(),date.atTime(session.getStartTime()).atZone(ZONE).toInstant(),date.atTime(session.getEndTime()).atZone(ZONE).toInstant(),true,TimeBlockKind.COURSE));
   }
   cs.add(new CourseSnapshot(c.getId().toString(),c.getTitle(),meetings));
  }
  var fixed=new ArrayList<com.studyplanner.contract.TimeBlock>();
  for(var w:windows.findByUserId(userId))if(w.getKind()!=com.studyplanner.enums.WindowKind.AVAILABLE){
   for(LocalDate date=today.minusDays(7);date.isBefore(today.plusDays(35));date=date.plusDays(1)){
    if(w.getLocalDate()!=null?!w.getLocalDate().equals(date):(w.getWeekday()==null||w.getWeekday()!=date.getDayOfWeek().getValue()))continue;
    fixed.add(new com.studyplanner.contract.TimeBlock("fixed-"+w.getId()+"-"+date,w.getId().toString(),date.atTime(w.getStartTime()).atZone(ZONE).toInstant(),date.atTime(w.getEndTime()).atZone(ZONE).toInstant(),true,w.getKind()==com.studyplanner.enums.WindowKind.BUSY?TimeBlockKind.UNAVAILABLE:TimeBlockKind.PERSONAL));
   }
  }
  return new ScheduleSnapshot(1,clock.instant(),user.getTimezone(),cs,ts,fixed,profile.activeFacts(),user.getPlanningRevision());
 }
 public static ScheduleSnapshot withTasks(ScheduleSnapshot s,List<TaskSnapshot> tasks){return new ScheduleSnapshot(s.schemaVersion(),s.now(),s.timezone(),s.courses(),tasks,s.lockedBlocks(),s.activeFacts(),s.planningRevision());}
}
