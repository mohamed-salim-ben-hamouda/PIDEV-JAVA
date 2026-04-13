package com.pidev.models;


import com.pidev.models.User;

import java.time.LocalDateTime;

public class Offer {
    private Integer id;
    private User entreprise;
    private String title;
    private String description;
    private String offerType;
    private String field;
    private String requiredLevel;
    private String requiredSkills;
    private String location;
    private String contractType;
    private Integer duration;
    private Double salaryRange;
    private String status;
    private LocalDateTime createdAt;

    public Offer() {
    }

    public Offer(Integer id) {
        this.id = id;
    }

    public Offer(Integer id, User entreprise, String title, String description, String offerType, String field,
                 String requiredLevel, String requiredSkills, String location, String contractType,
                 Integer duration, Double salaryRange, String status, LocalDateTime createdAt) {
        this.id = id;
        this.entreprise = entreprise;
        this.title = title;
        this.description = description;
        this.offerType = offerType;
        this.field = field;
        this.requiredLevel = requiredLevel;
        this.requiredSkills = requiredSkills;
        this.location = location;
        this.contractType = contractType;
        this.duration = duration;
        this.salaryRange = salaryRange;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(User entreprise) {
        this.entreprise = entreprise;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOfferType() {
        return offerType;
    }

    public void setOfferType(String offerType) {
        this.offerType = offerType;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(String requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Double getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(Double salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

