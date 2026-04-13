package com.pidev.models;

import java.time.LocalDate;

public class Education {
    private Integer id;
    private Cv cv;
    private String degree;
    private String fieldOfStudy;
    private String school;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;

    public Education() {
    }

    public Education(Integer id) {
        this.id = id;
    }

    public Education(Integer id, Cv cv, String degree, String fieldOfStudy, String school, String city,
                     LocalDate startDate, LocalDate endDate, String description) {
        this.id = id;
        this.cv = cv;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.school = school;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Cv getCv() {
        return cv;
    }

    public void setCv(Cv cv) {
        this.cv = cv;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
