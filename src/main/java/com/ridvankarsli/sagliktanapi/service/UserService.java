package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User getById(Long id);

    User updateProfile(Long userId, String firstName, String lastName, String bio);

    void deactivate(Long userId);

    // Gelişmiş arama: ad/soyada göre kişi arama.
    Page<User> search(String query, Pageable pageable);
}
