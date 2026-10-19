package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * 任务步骤 Repository
 * task_step 本表无 user_id，通过 task_id → task.user_id 间接归属
 */
public interface TaskStepRepository extends JpaRepository<TaskStep, Long> {
    List<TaskStep> findByTaskId(Long taskId);

    /** 按用户过滤：步骤所属任务归属于该用户 */
    @Query("select ts from TaskStep ts where ts.taskId in " +
           "(select t.id from Task t where t.userId = :userId)")
    Page<TaskStep> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 判断某步骤是否归属于该用户（用于单条数据权限校验） */
    @Query("select count(ts) > 0 from TaskStep ts where ts.id = :id and ts.taskId in " +
           "(select t.id from Task t where t.userId = :userId)")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
