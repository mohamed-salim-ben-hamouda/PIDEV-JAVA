package com.pidev.models;

import java.time.LocalDateTime;

public class CvApplication {
    private Integer id;
    private Offer offer;
    private Cv cv;
    private String status; // "PENDING", "ACCEPTED", "REJECTED"
    private LocalDateTime appliedAt;

    public CvApplication() {
        this.status = "PENDING";
        this.appliedAt = LocalDateTime.now();
    }

    public CvApplication(Integer id, Offer offer, Cv cv, String status, LocalDateTime appliedAt) {
        this.id = id;
        this.offer = offer;
        this.cv = cv;
        this.status = status;
        this.appliedAt = appliedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }

    public Cv getCv() {
        return cv;
    }

    public void setCv(Cv cv) {
        this.cv = cv;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
