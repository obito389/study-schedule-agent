package com.studyplanner.persistence;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface ScheduleRepository extends JpaRepository<ScheduleEntity,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScheduleEntity s where s.id = :id")
    Optional<ScheduleEntity> lockById(String id);
}
