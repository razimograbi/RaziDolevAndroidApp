package com.example.globalspeakclient;


/**
 * Represents a user in the system.
 *
 * This class stores user details including:
 * - Email, password, profile name, preferred language, user Embedding and gptCondLatent.
 *
 * Used in authentication and Firestore storage.
 */
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

public class User {
    private String email;
    private String password;
    private String profileName;
    private String language;
    private String embedding;
    private String gptCondLatent;

    public User() {
    }

    public User(String email, String password, String profileName, String language,
                String embedding, String gptCondLatent) {
        this.email = email;
        this.password = password;
        this.profileName = profileName;
        this.language = language;
        this.embedding = embedding;
        this.gptCondLatent = gptCondLatent;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public String getGptCondLatent() {
        return gptCondLatent;
    }

    public void setGptCondLatent(String gptCondLatent) {
        this.gptCondLatent = gptCondLatent;
    }
}
