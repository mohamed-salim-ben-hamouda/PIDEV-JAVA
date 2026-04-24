package com.pidev.utils;

import com.pidev.models.User;

public class SessionManager {
    private static User user;

    public static User getUser() {
        if (user == null) {
            // Mocking a logged in user for now
            user = new User(1);
            user.setNom("Omaima");
            user.setPrenom("Barhoumi");
            user.setEmail("barhoumi.omaima@esprit.tn"); 
        }
        return user;
    }

    public static void setUser(User user) {
        SessionManager.user = user;
    }
}
