package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "E-posta zorunludur")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        String email,

        @NotBlank(message = "Şifre zorunludur")
        @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
        String password,

        @NotBlank(message = "Ad zorunludur")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Soyad zorunludur")
        @Size(max = 100)
        String lastName
) {
}
