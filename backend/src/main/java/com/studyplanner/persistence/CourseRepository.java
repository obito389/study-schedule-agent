package com.studyplanner.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseRepository extends JpaRepository<CourseEntity,String> {
    List<CourseEntity> findByUserIdOrderById(String userId);

}
