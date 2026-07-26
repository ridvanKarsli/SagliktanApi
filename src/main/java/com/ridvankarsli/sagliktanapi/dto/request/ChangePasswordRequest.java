package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Mevcut şifre zorunludur")
        String currentPassword,

        @NotBlank(message = "Yeni şifre zorunludur")
        @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
        String newPassword
) {
}
