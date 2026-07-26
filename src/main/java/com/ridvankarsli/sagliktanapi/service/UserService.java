package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.User;

public interface UserService {

    User getById(Long id);

    User updateProfile(Long userId, String firstName, String lastName, String bio);

    void deactivate(Long userId);
}
