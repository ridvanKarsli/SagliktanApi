package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank(message = "Doğrulama kodu zorunludur")
        String code
) {
}
