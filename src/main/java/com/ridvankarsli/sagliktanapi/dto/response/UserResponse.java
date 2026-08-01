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
        LocalDateTime createdAt,
        long postCount,
        long commentCount,
        long likesReceived,
        long dislikesReceived
) {
    public static UserResponse from(User user) {
        return from(user, 0, 0, 0, 0);
    }

    // Profil sayfasındaki istatistik satırı için (bkz. UserController#getProfile) -
    // stats ayrı repository sorgularıyla hesaplanıp buraya taşınıyor, User
    // entity'sinde tutulmuyor (sürekli güncellenen sayaç alanı istemiyoruz).
    public static UserResponse from(User user, long postCount, long commentCount, long likesReceived, long dislikesReceived) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getRole(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                postCount,
                commentCount,
                likesReceived,
                dislikesReceived
        );
    }
}
