package com.studyplanner.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ImportRepository extends JpaRepository<ImportEntity,String> {
    List<ImportEntity> findByUserIdOrderById(String userId);

}
