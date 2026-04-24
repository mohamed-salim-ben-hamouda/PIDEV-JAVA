package com.pidev.models;

import java.time.LocalDateTime;

public class Participation {
    private Integer id;
    private String status;
    private String paymentStatus;
    private String paymentRef;
    private LocalDateTime registeredAt;
    private Hackathon hackathon;
    private Integer groupId; // group_id_id in database

    public Participation() {
        this.registeredAt = LocalDateTime.now();
        this.status = "pending";
        this.paymentStatus = "unpaid";
    }

    public Participation(Integer id, String status, String paymentStatus, String paymentRef, LocalDateTime registeredAt, Hackathon hackathon, Integer groupId) {
        this.id = id;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.paymentRef = paymentRef;
        this.registeredAt = registeredAt;
        this.hackathon = hackathon;
        this.groupId = groupId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Hackathon getHackathon() {
        return hackathon;
    }

    public void setHackathon(Hackathon hackathon) {
        this.hackathon = hackathon;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    @Override
    public String toString() {
        return "Participation{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", hackathon=" + (hackathon != null ? hackathon.getTitle() : "null") +
                '}';
    }
}
