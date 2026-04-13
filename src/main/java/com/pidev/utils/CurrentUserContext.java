package com.pidev.utils;

public final class CurrentUserContext {

    private static final int DUMMY_USER_ID = 23;

    private CurrentUserContext() {
    }

    public static int getCurrentUserId() {
        return DUMMY_USER_ID;
    }
}
