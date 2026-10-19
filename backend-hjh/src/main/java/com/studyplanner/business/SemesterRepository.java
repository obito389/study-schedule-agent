package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 学期 Repository
 */
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findByUserId(Long userId);
    Page<Semester> findByUserId(Long userId, Pageable pageable);
}
