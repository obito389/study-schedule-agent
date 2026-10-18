package com.studyplanner.persistence;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="action_execution")
public class ExecutionEntity {
    @Id @Column(length=80) public String id;
    @Column(nullable=false,length=80) public String userId;
    @Column(nullable=false,length=160) public String operationKey;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String requestJson;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String receiptJson;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String beforeJson;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String afterJson;
    public boolean undone;
    @Column(nullable=false) public Instant createdAt;
}
