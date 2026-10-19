package com.studyplanner.business;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * 课程实体（course 表）
 */
@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_id", nullable = false)
    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "课程标题不能为空")
    @Size(max = 100, message = "课程标题不能超过100个字符")
    private String title;

    @Column(length = 64)
    @Size(max = 64, message = "教师姓名不能超过64个字符")
    private String teacher;

    @Column(length = 100)
    @Size(max = 100, message = "上课地点不能超过100个字符")
    private String location;

    public Course() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
