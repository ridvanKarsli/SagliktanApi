package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank(message = "Sıfırlama kodu zorunludur")
        String code,

        @NotBlank(message = "Yeni şifre zorunludur")
        @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
        String newPassword
) {
}
