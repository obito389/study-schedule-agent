package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LearningStatRepository extends JpaRepository<LearningStat, Long> {
    List<LearningStat> findByUserId(Long userId);
    Page<LearningStat> findByUserId(Long userId, Pageable pageable);
}
