package com.pidev.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String passwd;
    private LocalDate date_naissance;
    private LocalDateTime date_inscrit;
    private boolean is_active = true;
    private String photo;
    private Role role;
    private boolean isConnected;
    private LocalDateTime banUntil;

    public enum Role {
        STUDENT, SUPERVISEUR, ENTREPRISE, ADMIN
    }

    public User() {
        this.date_inscrit = LocalDateTime.now();
        this.is_active = true;
    }

    public User(Integer id) {
        this.id = id;
    }
    
    public User(String nom, String prenom, String email, String passwd, LocalDate date_naissance, Role role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.passwd = passwd;
        this.date_naissance = date_naissance;
        this.role = role;
        this.date_inscrit = LocalDateTime.now();
        this.is_active = true;
    }

    public String getDisplayName() {
        if (prenom != null && nom != null) {
            return prenom + " " + nom;
        }
        if (email != null) {
            return email;
        }
        return "Unknown";
    }

    // --- Getters and Setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswd() { return passwd; }
    public void setPasswd(String passwd) { this.passwd = passwd; }

    public LocalDate getDateNaissance() { return date_naissance; }
    public void setDateNaissance(LocalDate date_naissance) { this.date_naissance = date_naissance; }

    public LocalDateTime getDateInscrit() { return date_inscrit; }
    public void setDateInscrit(LocalDateTime date_inscrit) { this.date_inscrit = date_inscrit; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public boolean isActive() { return is_active; }
    public void setActive(boolean is_active) { this.is_active = is_active; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { isConnected = connected; }

    public LocalDateTime getBanUntil() { return banUntil; }
    public void setBanUntil(LocalDateTime banUntil) { this.banUntil = banUntil; }

    public boolean isBanned() {
        return banUntil != null && banUntil.isAfter(LocalDateTime.now());
    }
}