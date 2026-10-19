package com.studyplanner.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PreviewRepository extends JpaRepository<PreviewEntity,String> {
    List<PreviewEntity> findByUserIdOrderById(String userId);

}
