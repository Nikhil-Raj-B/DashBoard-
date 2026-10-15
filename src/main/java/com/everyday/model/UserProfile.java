package com.everyday.model;

public class UserProfile {
    private int id = 1;
    private String name;
    private String tagline;
    private String avatarPath;

    public UserProfile() {
        this.name = "Nikhil Raj";
        this.tagline = "Personal";
    }

    public UserProfile(int id, String name, String tagline, String avatarPath) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.avatarPath = avatarPath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
}
