package com.studyplanner.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FactRepository extends JpaRepository<FactEntity,String> {
    List<FactEntity> findByUserIdOrderById(String userId);

}
