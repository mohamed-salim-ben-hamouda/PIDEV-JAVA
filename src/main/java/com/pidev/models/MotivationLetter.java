package com.pidev.models;

import java.time.LocalDateTime;

public class MotivationLetter {
    private Integer id;
    private String content;
    private Integer cvId;
    private Integer offerId;
    private LocalDateTime createdAt;

    public MotivationLetter() {
        this.createdAt = LocalDateTime.now();
    }

    public MotivationLetter(Integer id, String content, Integer cvId, Integer offerId, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.cvId = cvId;
        this.offerId = offerId;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getCvId() { return cvId; }
    public void setCvId(Integer cvId) { this.cvId = cvId; }

    public Integer getOfferId() { return offerId; }
    public void setOfferId(Integer offerId) { this.offerId = offerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
