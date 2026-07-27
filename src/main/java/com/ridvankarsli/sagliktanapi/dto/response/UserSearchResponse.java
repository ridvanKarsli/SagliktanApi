package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.User;

// Kişi aramasında dönen, gizlilik açısından güvenli özet - e-posta veya
// başka hassas alan içermez (bkz. UserResponse, o sadece /api/users/me için).
public record UserSearchResponse(
        Long id,
        String firstName,
        String lastName,
        String bio
) {
    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio()
        );
    }
}
