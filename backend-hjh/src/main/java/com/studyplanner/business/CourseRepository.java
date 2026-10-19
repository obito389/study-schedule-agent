package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * 课程 Repository
 * course 本表无 user_id，通过 semester_id → semester.user_id 间接归属
 */
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySemesterId(Long semesterId);

    /** 按用户过滤：课程所属学期归属于该用户 */
    @Query("select c from Course c where c.semesterId in " +
           "(select s.id from Semester s where s.userId = :userId)")
    Page<Course> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 判断某课程是否归属于该用户（用于单条数据权限校验） */
    @Query("select count(c) > 0 from Course c where c.id = :id and c.semesterId in " +
           "(select s.id from Semester s where s.userId = :userId)")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
