package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String firstName, String lastName, String bio) {
        User user = getById(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBio(bio);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivate(Long userId) {
        User user = getById(userId);
        user.setActive(false);
        userRepository.save(user);
    }
}
