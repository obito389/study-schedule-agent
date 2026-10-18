package com.studyplanner.persistence;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="ai_draft")
public class AiDraftEntity {
    @Id @Column(length=80) public String id;
    @Column(nullable=false,length=80) public String userId;
    public long baseRevision;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String previewJson;
    @Column(columnDefinition="LONGTEXT") public String commandJson;
    @Column(nullable=false) public Instant createdAt;
}
