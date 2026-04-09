package com.pidev.models;

public class SponsorHackathon {
    private Integer id;
    private Sponsor sponsor;
    private Hackathon hackathon;
    private String contributionType;
    private Double contributionValue;

    public SponsorHackathon() {
    }

    public SponsorHackathon(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Sponsor getSponsor() {
        return sponsor;
    }

    public void setSponsor(Sponsor sponsor) {
        this.sponsor = sponsor;
    }

    public Hackathon getHackathon() {
        return hackathon;
    }

    public void setHackathon(Hackathon hackathon) {
        this.hackathon = hackathon;
    }

    public String getContributionType() {
        return contributionType;
    }

    public void setContributionType(String contributionType) {
        this.contributionType = contributionType;
    }

    public Double getContributionValue() {
        return contributionValue;
    }

    public void setContributionValue(Double contributionValue) {
        this.contributionValue = contributionValue;
    }
}
