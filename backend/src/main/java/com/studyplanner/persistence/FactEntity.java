package com.studyplanner.persistence;
import jakarta.persistence.*;
@Entity @Table(name="profile_fact_record")
public class FactEntity {
    @Id @Column(length=80) public String id;
    @Column(nullable=false,length=80) public String userId;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String factJson;
    public long factVersion;
}
