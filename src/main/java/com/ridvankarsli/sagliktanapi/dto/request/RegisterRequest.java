package com.ridvankarsli.sagliktanapi.dto.request;

import com.ridvankarsli.sagliktanapi.validation.ValidName;
import jakarta.validation.constraints.AssertTrue;
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
        @ValidName
        String firstName,

        @NotBlank(message = "Soyad zorunludur")
        @ValidName
        String lastName,

        // KVKK aydınlatma metni + açık rıza onayı olmadan kayıt tamamlanamaz.
        // Frontend'in kayıt formunda bu alanı true olarak göndermesi gerekir.
        @AssertTrue(message = "Kayıt olmak için KVKK Aydınlatma Metni'ni onaylamanız gerekir")
        boolean kvkkConsent
) {
}
