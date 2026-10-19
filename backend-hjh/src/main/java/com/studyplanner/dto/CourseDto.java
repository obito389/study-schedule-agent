package com.studyplanner.dto;

import com.studyplanner.business.Course;

public class CourseDto {
    private Long id;
    private Long semesterId;
    private String title;
    private String teacher;
    private String location;

    public static CourseDto from(Course entity) {
        CourseDto dto = new CourseDto();
        dto.setId(entity.getId());
        dto.setSemesterId(entity.getSemesterId());
        dto.setTitle(entity.getTitle());
        dto.setTeacher(entity.getTeacher());
        dto.setLocation(entity.getLocation());
        return dto;
    }

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
