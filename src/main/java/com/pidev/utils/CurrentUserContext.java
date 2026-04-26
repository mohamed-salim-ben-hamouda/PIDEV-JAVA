package com.pidev.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CurrentUserContext {

    private static final int NO_USER = -1;
    private static volatile int currentUserId = NO_USER;
    private static final List<UserChangeListener> listeners = new CopyOnWriteArrayList<>();

    private CurrentUserContext() {
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static boolean isLoggedIn() {
        return currentUserId > 0;
    }

    public static void loginAs(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be > 0.");
        }
        currentUserId = userId;
        notifyListeners(userId);
    }

    public static void logout() {
        currentUserId = NO_USER;
        notifyListeners(NO_USER);
    }

    public static void addListener(UserChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void removeListener(UserChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(int newUserId) {
        for (UserChangeListener listener : listeners) {
            try {
                listener.onUserChanged(newUserId);
            } catch (Exception ignored) {
            }
        }
    }

    @FunctionalInterface
    public interface UserChangeListener {
        void onUserChanged(int newUserId);
    }
}
