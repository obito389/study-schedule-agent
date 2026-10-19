package com.studyplanner.persistence;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="calendar_block")
public class BlockEntity {
    @Id @Column(length=80) public String id;
    @Column(nullable=false,length=80) public String userId;
    @Column(nullable=false,length=80) public String ownerId;
    @Column(name="starts_at",nullable=false) public Instant start;
    @Column(name="ends_at",nullable=false) public Instant end;
    public boolean locked;
    @Column(nullable=false,length=30) public String kind;
}
