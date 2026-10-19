package com.studyplanner.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BlockRepository extends JpaRepository<BlockEntity,String> {
    List<BlockEntity> findByUserIdOrderById(String userId);

}
