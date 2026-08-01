package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.User;

// Kişi aramasında dönen, gizlilik açısından güvenli özet - e-posta veya
// başka hassas alan içermez (bkz. UserResponse, o sadece /api/users/me için).
public record UserSearchResponse(
        Long id,
        String firstName,
        String lastName,
        String bio,
        long postCount,
        long commentCount,
        long likesReceived,
        long dislikesReceived
) {
    public static UserSearchResponse from(User user) {
        return from(user, 0, 0, 0, 0);
    }

    // Herkese açık profilde gösterilen istatistik satırı için - bkz. UserController#getPublicProfile.
    public static UserSearchResponse from(User user, long postCount, long commentCount, long likesReceived, long dislikesReceived) {
        return new UserSearchResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                postCount,
                commentCount,
                likesReceived,
                dislikesReceived
        );
    }
}
