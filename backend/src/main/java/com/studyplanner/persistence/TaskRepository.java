package com.studyplanner.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TaskRepository extends JpaRepository<TaskEntity,String> {
    List<TaskEntity> findByUserIdOrderById(String userId);

}
