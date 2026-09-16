package com.studyplanner.planning;

import com.studyplanner.contract.CourseSnapshot;
import com.studyplanner.contract.ProfileFact;
import com.studyplanner.contract.ScheduleSnapshot;
import com.studyplanner.contract.TaskSnapshot;
import com.studyplanner.contract.TimeBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 硬约束校验规则测试：截止、重叠、锁定块、每日上限、跨午夜与确定性。
 */
class HardConstraintValidatorTest {

    private static final Instant T10 = Instant.parse("2026-09-20T10:00:00Z");

    private HardConstraintValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HardConstraintValidator();
    }

    private ScheduleSnapshot snapshot(List<CourseSnapshot> courses, List<TaskSnapshot> tasks,
                                      List<TimeBlock> locked, List<ProfileFact> facts) {
        return new ScheduleSnapshot(1, T10, "Asia/Shanghai", courses, tasks, locked, facts, 1);
    }

    private TaskSnapshot task(String id, Instant deadline, int minutes, TimeBlock scheduled) {
        return new TaskSnapshot(1, id, "任务-" + id, null, deadline, minutes, 1, "PENDING", scheduled);
    }

    @Test
    void deadlineExceededFails() {
        ScheduleSnapshot s = snapshot(List.of(), List.of(task("t1", T10.plusSeconds(3600), 90, null)), List.of(), List.of());
        CandidateBlock c = new CandidateBlock("t1", T10, T10.plusSeconds(5400));
        List<ConstraintViolation> vs = validator.validate(s, c);
        assertTrue(vs.stream().anyMatch(v -> HardConstraintValidator.DEADLINE_EXCEEDED.equals(v.code())));
    }

    @Test
    void deadlineEqualAllowed() {
        // 结束时间恰好等于截止时间，允许
        ScheduleSnapshot s = snapshot(List.of(), List.of(task("t1", T10.plusSeconds(3600), 60, null)), List.of(), List.of());
        CandidateBlock c = new CandidateBlock("t1", T10, T10.plusSeconds(3600));
        assertTrue(validator.validate(s, c).isEmpty());
    }

    @Test
    void overlapWithOtherTaskFails() {
        TimeBlock other = new TimeBlock(1, "b1", T10, T10.plusSeconds(3600), false, "已排");
        ScheduleSnapshot s = snapshot(List.of(),
                List.of(task("t1", null, 60, null), task("t2", null, 60, other)), List.of(), List.of());
        CandidateBlock c = new CandidateBlock("t1", T10.plusSeconds(1800), T10.plusSeconds(3600));
        List<ConstraintViolation> vs = validator.validate(s, c);
        assertTrue(vs.stream().anyMatch(v -> HardConstraintValidator.OVERLAP.equals(v.code())));
    }

    @Test
    void adjacentBoundaryAllowed() {
        // 与前一个块恰好相邻（首尾相接）不算重叠
        TimeBlock other = new TimeBlock(1, "b1", T10, T10.plusSeconds(3600), false, "已排");
        ScheduleSnapshot s = snapshot(List.of(),
                List.of(task("t1", null, 60, null), task("t2", null, 60, other)), List.of(), List.of());
        CandidateBlock c = new CandidateBlock("t1", T10.plusSeconds(3600), T10.plusSeconds(5400));
        assertTrue(validator.validate(s, c).isEmpty());
    }

    @Test
    void lockedBlockFails() {
        TimeBlock locked = new TimeBlock(1, "L1", T10, T10.plusSeconds(3600), true, "周一上课");
        ScheduleSnapshot s = snapshot(List.of(), List.of(task("t1", null, 60, null)), List.of(locked), List.of());
        CandidateBlock c = new CandidateBlock("t1", T10.plusSeconds(1800), T10.plusSeconds(3600));
        List<ConstraintViolation> vs = validator.validate(s, c);
        assertTrue(vs.stream().anyMatch(v -> HardConstraintValidator.LOCKED_BLOCK.equals(v.code())));
    }

    @Test
    void courseWeeklyBlockIsLocked() {
        TimeBlock lecture = new TimeBlock(1, "b1", T10, T10.plusSeconds(7200), true, "数据结构 10:00-12:00");
        CourseSnapshot course = new CourseSnapshot(1, "c1", "CS201", "数据结构", List.of(lecture));
        ScheduleSnapshot s = snapshot(List.of(course), List.of(task("t1", null, 60, null)), List.of(), List.of());
        CandidateBlock c = new CandidateBlock("t1", T10.plusSeconds(1800), T10.plusSeconds(3600));
        List<ConstraintViolation> vs = validator.validate(s, c);
        assertTrue(vs.stream().anyMatch(v -> HardConstraintValidator.LOCKED_BLOCK.equals(v.code())));
    }

    @Test
    void dailyCapExceededFails() {
        ProfileFact cap = new ProfileFact(1, "daily.cap.minutes", "120", "GLOBAL", 1.0, "USER_INPUT", "ACTIVE", List.of(), 1);
        TimeBlock occupied = new TimeBlock(1, "b1", T10, T10.plusSeconds(5400), true, "已占用 90 分钟");
        ScheduleSnapshot s = snapshot(List.of(), List.of(task("t1", null, 60, null)), List.of(occupied), List.of(cap));
        CandidateBlock c = new CandidateBlock("t1", T10.plusSeconds(5400), T10.plusSeconds(9000));
        List<ConstraintViolation> vs = validator.validate(s, c);
        assertTrue(vs.stream().anyMatch(v -> HardConstraintValidator.DAILY_CAP_EXCEEDED.equals(v.code())));
    }

    @Test
    void dailyCapWithinLimitAllowed() {
        ProfileFact cap = new ProfileFact(1, "daily.cap.minutes", "300", "GLOBAL", 1.0, "USER_INPUT", "ACTIVE", List.of(), 1);
        TimeBlock occupied = new TimeBlock(1, "b1", T10, T10.plusSeconds(5400), true, "已占用 90 分钟");
        ScheduleSnapshot s = snapshot(List.of(), List.of(task("t1", null, 120, null)), List.of(occupied), List.of(cap));
        CandidateBlock c = new CandidateBlock("t1", T10.plusSeconds(5400), T10.plusSeconds(9000));
        assertTrue(validator.validate(s, c).isEmpty());
    }

    @Test
    void crossMidnightCountsByStartDate() {
        // 跨午夜的块按开始日期计入当天上限
        Instant evening = Instant.parse("2026-09-20T14:00:00Z"); // 22:00 北京
        Instant nextDay = Instant.parse("2026-09-21T02:00:00Z"); // 次日 10:00 北京
        ProfileFact cap = new ProfileFact(1, "daily.cap.minutes", "60", "GLOBAL", 1.0, "USER_INPUT", "ACTIVE", List.of(), 1);
        ScheduleSnapshot s = snapshot(List.of(), List.of(task("t1", null, 30, null)), List.of(), List.of(cap));
        // 当天已有 50 分钟占用，加 30 分钟候选超过 60 → 失败
        TimeBlock occupied = new TimeBlock(1, "b1", evening, evening.plusSeconds(3000), true, "50 分钟");
        ScheduleSnapshot withOcc = snapshot(List.of(), List.of(task("t1", null, 30, null)), List.of(occupied), List.of(cap));
        CandidateBlock c = new CandidateBlock("t1", evening.plusSeconds(3600), evening.plusSeconds(5400));
        assertTrue(validator.validate(withOcc, c).stream()
                .anyMatch(v -> HardConstraintValidator.DAILY_CAP_EXCEEDED.equals(v.code())));
        // 跨午夜块本身不破坏计算
        assertFalse(nextDay.isBefore(evening));
    }

    @Test
    void deterministicOrderForSameInput() {
        ProfileFact cap = new ProfileFact(1, "daily.cap.minutes", "60", "GLOBAL", 1.0, "USER_INPUT", "ACTIVE", List.of(), 1);
        TimeBlock locked = new TimeBlock(1, "L1", T10, T10.plusSeconds(3600), true, "锁定");
        TimeBlock occupied = new TimeBlock(1, "b1", T10.plusSeconds(3600), T10.plusSeconds(7200), false, "已排");
        ScheduleSnapshot s = snapshot(List.of(),
                List.of(task("t1", T10.plusSeconds(1800), 120, null), task("t2", null, 60, occupied)),
                List.of(locked), List.of(cap));
        CandidateBlock c = new CandidateBlock("t1", T10, T10.plusSeconds(7200));
        List<ConstraintViolation> first = validator.validate(s, c);
        for (int i = 0; i < 5; i++) {
            assertEquals(first, validator.validate(s, c), "相同输入必须返回相同顺序的违反项");
        }
        assertEquals(1, first.stream().filter(v -> HardConstraintValidator.DEADLINE_EXCEEDED.equals(v.code())).count());
    }
}
