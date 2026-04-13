package com.pidev.models;


public class Langue {
    private Integer id;
    private Cv cv;
    private String nom;
    private String niveau;

    public Langue() {
    }

    public Langue(Integer id) {
        this.id = id;
    }

    public Langue(Integer id, Cv cv, String nom, String niveau) {
        this.id = id;
        this.cv = cv;
        this.nom = nom;
        this.niveau = niveau;
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

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }
}
