package com.pidev.models;

import com.pidev.models.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Cv {
    private Integer id;
    private String nomCv;
    private String langue;
    private Integer idTemplate;
    private Integer progression;
    private LocalDateTime creationDate;
    private LocalDateTime updatedAt;
    private User user;
    private String linkedinUrl;
    private String summary;
    private List<Experience> experiences;
    private List<Education> educations;
    private List<Skill> skills;
    private List<Certif> certifs;
    private List<Langue> languages;

    public Cv() {
        this.experiences = new ArrayList<>();
        this.educations = new ArrayList<>();
        this.skills = new ArrayList<>();
        this.certifs = new ArrayList<>();
        this.languages = new ArrayList<>();
    }

    public Cv(Integer id) {
        this();
        this.id = id;
    }

    public Cv(Integer id, String nomCv, String langue, Integer idTemplate, Integer progression,
              LocalDateTime creationDate, LocalDateTime updatedAt, User user, String linkedinUrl,
              String summary, List<Experience> experiences, List<Education> educations,
              List<Skill> skills, List<Certif> certifs, List<Langue> languages) {
        this.id = id;
        this.nomCv = nomCv;
        this.langue = langue;
        this.idTemplate = idTemplate;
        this.progression = progression;
        this.creationDate = creationDate;
        this.updatedAt = updatedAt;
        this.user = user;
        this.linkedinUrl = linkedinUrl;
        this.summary = summary;
        this.experiences = experiences != null ? experiences : new ArrayList<>();
        this.educations = educations != null ? educations : new ArrayList<>();
        this.skills = skills != null ? skills : new ArrayList<>();
        this.certifs = certifs != null ? certifs : new ArrayList<>();
        this.languages = languages != null ? languages : new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomCv() {
        return nomCv;
    }

    public void setNomCv(String nomCv) {
        this.nomCv = nomCv;
    }

    public String getLangue() {
        return langue;
    }

    public void setLangue(String langue) {
        this.langue = langue;
    }

    public Integer getIdTemplate() {
        return idTemplate;
    }

    public void setIdTemplate(Integer idTemplate) {
        this.idTemplate = idTemplate;
    }

    public Integer getProgression() {
        return calculateProgression();
    }

    public int calculateProgression() {
        int progress = 0;
        if (experiences != null && !experiences.isEmpty()) progress += 20;
        if (educations != null && !educations.isEmpty()) progress += 20;
        if (skills != null && !skills.isEmpty()) progress += 20;
        if (certifs != null && !certifs.isEmpty()) progress += 20;
        if (languages != null && !languages.isEmpty()) progress += 20;
        return progress;
    }

    public void setProgression(Integer progression) {
        this.progression = progression;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Experience> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<Experience> experiences) {
        this.experiences = experiences;
    }

    public List<Education> getEducations() {
        return educations;
    }

    public void setEducations(List<Education> educations) {
        this.educations = educations;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }

    public List<Certif> getCertifs() {
        return certifs;
    }

    public void setCertifs(List<Certif> certifs) {
        this.certifs = certifs;
    }

    public List<Langue> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Langue> languages) {
        this.languages = languages;
    }
}
