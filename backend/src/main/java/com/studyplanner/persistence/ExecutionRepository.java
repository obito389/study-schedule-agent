package com.studyplanner.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExecutionRepository extends JpaRepository<ExecutionEntity,String> {
    List<ExecutionEntity> findByUserIdOrderById(String userId);
    java.util.Optional<ExecutionEntity> findByUserIdAndOperationKey(String userId, String operationKey);
}
