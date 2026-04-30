package com.pidev.models;

import java.time.LocalDateTime;

public class Notif {
    private int id;
    private String message;
    private boolean is_read;
    private LocalDateTime created_at;
    private int user_id;

    public Notif() {
        this.created_at = LocalDateTime.now();
        this.is_read = false;
    }

    public Notif(int user_id, String message) {
        this();
        this.user_id = user_id;
        this.message = message;
    }

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isIs_read() { return is_read; }
    public void setIs_read(boolean is_read) { this.is_read = is_read; }

    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }
}
