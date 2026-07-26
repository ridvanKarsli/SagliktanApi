package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.service.AuthTokens;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static AuthResponse from(AuthTokens tokens) {
        return new AuthResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer");
    }
}
