package com.studyplanner.persistence;
import jakarta.persistence.*;
@Entity @Table(name="import_job")
public class ImportEntity {
    @Id @Column(length=160) public String id;
    @Column(nullable=false,length=80) public String userId;
    @Column(nullable=false,columnDefinition="LONGTEXT") public String jobJson;
}
