package com.pidev.utils;

import com.pidev.models.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getUser() {
        return currentUser;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void cleanUserSession() {
        this.currentUser = null;
    }

    public void logout() {
        cleanUserSession();
    }

    public boolean isLogged() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return isLogged() && currentUser.getRole() == User.Role.ADMIN;
    }
}
