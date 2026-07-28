package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.domain.User;

import java.time.LocalDateTime;

// Admin paneline özel: UserResponse'tan farklı olarak active/kvkkConsentAt
// da içerir (admin görmeli), ama passwordHash/verificationCode/resetCode
// yine de asla döndürülmez.
public record AdminUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String bio,
        Role role,
        boolean emailVerified,
        boolean active,
        LocalDateTime kvkkConsentAt,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getBio(),
                u.getRole(),
                u.isEmailVerified(),
                u.isActive(),
                u.getKvkkConsentAt(),
                u.getCreatedAt()
        );
    }
}
