package com.sai.decisiongraveyard.model;

import com.google.firebase.Timestamp;

public class User {

    private String userId;
    private String email;
    private Timestamp createdAt;

    public User() {
    }

    public User(String userId, String email, Timestamp createdAt) {
        this.userId = userId;
        this.email = email;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
