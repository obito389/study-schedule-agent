package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * 课程节次 Repository
 * course_session 本表无 user_id，通过 course_id → course → semester.user_id 间接归属
 */
public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {
    List<CourseSession> findByCourseId(Long courseId);

    /** 按用户过滤：节次所属课程（再到学期）归属于该用户 */
    @Query("select cs from CourseSession cs where cs.courseId in " +
           "(select c.id from Course c where c.semesterId in " +
           "(select s.id from Semester s where s.userId = :userId))")
    Page<CourseSession> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 判断某节次是否归属于该用户（用于单条数据权限校验） */
    @Query("select count(cs) > 0 from CourseSession cs where cs.id = :id and cs.courseId in " +
           "(select c.id from Course c where c.semesterId in " +
           "(select s.id from Semester s where s.userId = :userId))")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
