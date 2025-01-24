package com.example.globalspeakclient;

public class ActiveUser {
    private String userId;
    private String profileName;
    private String email;

    public ActiveUser(String userId, String profileName, String email) {
        this.userId = userId;
        this.profileName = profileName;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getEmail() {
        return email;
    }
}

