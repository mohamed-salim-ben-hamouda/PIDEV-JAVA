package com.pidev.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private Integer id;
    private String nom;
    private String prenom;
    private LocalDate date_naissance; // Matches DATE_MUTABLE
    private String email;
    private boolean ban = false;
    private String photo;
    private String passwd;
    private LocalDateTime date_inscrit; // Matches DATETIME_MUTABLE
    private boolean is_active = true;
    private int report_nbr = 0;
    private String previous_role;
    private LocalDateTime banned_until;
    private boolean archived = false;
    private String webauthn_credentialId;
    private String webauthn_public_key;
    private String face_descriptor;

    public User() {
        this.ban = false;
        this.report_nbr = 0;
        this.is_active = true;
        this.archived = false;
        this.date_inscrit = LocalDateTime.now();
    }

    public User(int id) {
        this.id = id;
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

    public LocalDate getDateNaissance() { return date_naissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.date_naissance = dateNaissance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isBan() { return ban; }
    public void setBan(boolean ban) { this.ban = ban; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getPasswd() { return passwd; }
    public void setPasswd(String passwd) { this.passwd = passwd; }

    public LocalDateTime getDateInscrit() { return date_inscrit; }
    public void setDateInscrit(LocalDateTime dateInscrit) { this.date_inscrit = dateInscrit; }

    public boolean isActive() { return is_active; }
    public void setActive(boolean active) { is_active = active; }

    public int getReportNbr() { return report_nbr; }
    public void setReportNbr(int reportNbr) { this.report_nbr = reportNbr; }

    public String getPreviousRole() { return previous_role; }
    public void setPreviousRole(String previousRole) { this.previous_role = previousRole; }

    public LocalDateTime getBannedUntil() { return banned_until; }
    public void setBannedUntil(LocalDateTime bannedUntil) { this.banned_until = bannedUntil; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public String getWebauthnCredentialId() { return webauthn_credentialId; }
    public void setWebauthnCredentialId(String webauthnCredentialId) { this.webauthn_credentialId = webauthnCredentialId; }

    public String getWebauthnPublicKey() { return webauthn_public_key; }
    public void setWebauthnPublicKey(String webauthnPublicKey) { this.webauthn_public_key = webauthnPublicKey; }

    public String getFaceDescriptor() { return face_descriptor; }
    public void setFaceDescriptor(String faceDescriptor) { this.face_descriptor = faceDescriptor; }
}