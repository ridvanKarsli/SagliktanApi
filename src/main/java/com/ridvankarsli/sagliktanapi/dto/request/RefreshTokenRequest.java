package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token zorunludur")
        String refreshToken
) {
}
