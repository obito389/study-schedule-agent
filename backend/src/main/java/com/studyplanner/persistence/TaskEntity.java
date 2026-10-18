package com.studyplanner.persistence;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="study_task")
public class TaskEntity {
    @Id @Column(length=80) public String id;
    @Column(nullable=false,length=80) public String userId;
    @Column(length=80) public String courseId;
    @Column(nullable=false,length=200) public String title;
    @Column(nullable=false) public Instant deadline;
    public int durationMinutes;
    public int priority;
    @Column(nullable=false,length=30) public String status;
}
