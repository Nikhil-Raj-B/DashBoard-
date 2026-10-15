package com.everyday.service;

import com.everyday.model.UserProfile;
import com.everyday.repository.ProfileRepository;

import java.sql.SQLException;

public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public UserProfile getProfile() throws SQLException {
        return profileRepository.getProfile();
    }

    public void updateProfile(String name, String tagline, String avatarPath) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            name = "User";
        }
        if (tagline == null || tagline.trim().isEmpty()) {
            tagline = "Personal";
        }
        UserProfile profile = new UserProfile(1, name.trim(), tagline.trim(), avatarPath);
        profileRepository.updateProfile(profile);
    }
}
