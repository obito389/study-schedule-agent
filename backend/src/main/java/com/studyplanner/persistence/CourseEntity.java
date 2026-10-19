package com.studyplanner.persistence;
import jakarta.persistence.*;
@Entity @Table(name="study_course")
public class CourseEntity {
    @Id @Column(length=80) public String id;
    @Column(nullable=false,length=80) public String userId;
    @Column(nullable=false,length=200) public String name;
}
