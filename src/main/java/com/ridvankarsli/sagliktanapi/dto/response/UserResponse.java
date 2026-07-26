package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.domain.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String bio,
        Role role,
        boolean emailVerified,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getRole(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
