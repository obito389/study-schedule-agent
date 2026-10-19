package com.studyplanner.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 时间块 Repository
 */
public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {
    List<TimeBlock> findByUserId(Long userId);
    List<TimeBlock> findByUserIdAndStartAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
    Page<TimeBlock> findByUserId(Long userId, Pageable pageable);

    /**
     * 查询与给定区间 [start, end] 时间重叠的时间块（重叠条件：b.startAt < end 且 b.endAt > start）。
     * 状态是否 CANCELLED 由调用方在内存中过滤。
     */
    @Query("select b from TimeBlock b where b.userId = :userId and b.startAt < :end and b.endAt > :start")
    List<TimeBlock> findOverlapping(@Param("userId") Long userId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);
}
