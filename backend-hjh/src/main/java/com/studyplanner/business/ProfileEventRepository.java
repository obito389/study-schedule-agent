package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileEventRepository extends JpaRepository<ProfileEvent, Long> {
    List<ProfileEvent> findByUserId(Long userId);
    Page<ProfileEvent> findByUserId(Long userId, Pageable pageable);
}
