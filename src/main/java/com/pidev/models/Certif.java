package com.pidev.models;
import java.time.LocalDate;

public class Certif {
    private Integer id;
    private Cv cv;
    private String name;
    private String issuedBy;
    private LocalDate issueDate;
    private LocalDate expDate;

    public Certif() {
    }

    public Certif(Integer id) {
        this.id = id;
    }

    public Certif(Integer id, Cv cv, String name, String issuedBy, LocalDate issueDate, LocalDate expDate) {
        this.id = id;
        this.cv = cv;
        this.name = name;
        this.issuedBy = issuedBy;
        this.issueDate = issueDate;
        this.expDate = expDate;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpDate() {
        return expDate;
    }

    public void setExpDate(LocalDate expDate) {
        this.expDate = expDate;
    }
}

