package com.example.globalspeakclient;


/**
 * Represents an active user in the system.
 * Stores user identification, profile name, and email.
 */
public class ActiveUser {
    private String userId;
    private String profileName;
    private String email;


    /**
     * Creates an ActiveUser with the given details.
     * @param userId Unique user identifier.
     * @param profileName User's display name.
     * @param email User's email address.
     */
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

