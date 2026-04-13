package com.pidev.models;

public class Skill {
    private Integer id;
    private Cv cv;
    private String nom;
    private String type;
    private String level;

    public Skill() {
    }

    public Skill(Integer id) {
        this.id = id;
    }

    public Skill(Integer id, Cv cv, String nom, String type, String level) {
        this.id = id;
        this.cv = cv;
        this.nom = nom;
        this.type = type;
        this.level = level;
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

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
