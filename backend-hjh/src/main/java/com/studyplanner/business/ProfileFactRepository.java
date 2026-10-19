package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileFactRepository extends JpaRepository<ProfileFact, Long> {
    List<ProfileFact> findByUserId(Long userId);
    List<ProfileFact> findByUserIdAndKey(Long userId, String key);
    Page<ProfileFact> findByUserId(Long userId, Pageable pageable);
}
