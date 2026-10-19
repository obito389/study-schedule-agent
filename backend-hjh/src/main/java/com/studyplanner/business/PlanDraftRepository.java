package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanDraftRepository extends JpaRepository<PlanDraft, Long> {
    List<PlanDraft> findByUserId(Long userId);
    List<PlanDraft> findByUserIdAndStatus(Long userId, String status);
    Page<PlanDraft> findByUserId(Long userId, Pageable pageable);
}
