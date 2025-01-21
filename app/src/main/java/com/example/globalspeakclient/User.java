package com.example.globalspeakclient;


/**
 * Represents a user in the system.
 *
 * This class stores user details including:
 * - Email, password, profile name, and preferred language.
 * - Optional fields: audio file URL (previous method) and embedding vector.
 *
 * Used in authentication and Firestore storage.
 */
public class User {

    private final String email;
    private final String password;
    private final String profileName;
    private final String language;
    private String audioFileUrl;
    private String embedding;

    public User(String email, String password, String profileName, String language) {
        this.email = email;
        this.password = password;
        this.profileName = profileName;
        this.language = language;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getLanguage() {
        return language;
    }

    public String getAudioFileUrl() {
        return audioFileUrl;
    }

    public void setAudioFileUrl(String audioFileUrl) {
        this.audioFileUrl = audioFileUrl;
    }


    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
}
