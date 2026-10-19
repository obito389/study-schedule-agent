package com.studyplanner.persistence;
import jakarta.persistence.*;
@Entity @Table(name="user_schedule")
public class ScheduleEntity {
    @Id @Column(length=80) public String id;
    public long revision;
    @Column(nullable=false, length=80) public String timezone;
}
