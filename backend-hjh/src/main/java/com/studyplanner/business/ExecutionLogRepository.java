package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    List<ExecutionLog> findByUserId(Long userId);
    List<ExecutionLog> findByTaskId(Long taskId);
    Page<ExecutionLog> findByUserId(Long userId, Pageable pageable);
}
