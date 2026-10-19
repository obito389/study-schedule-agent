package com.studyplanner.persistence;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="plan_preview")
public class PreviewEntity {
    @Id @Column(length=80) public String id;
    @Column(nullable=false,length=80) public String userId;
    public long baseRevision;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String candidateJson;
    @Column(nullable=false) public Instant createdAt;
}
