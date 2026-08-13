package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotBlank;

// Hesap silme geri alınamaz olduğu için (bkz. UserServiceImpl.deleteAccount)
// mevcut şifre ile teyit istenir - aynı ChangePasswordRequest desenindeki
// gibi (bkz. AuthServiceImpl.changePassword).
public record DeleteAccountRequest(

        @NotBlank(message = "Şifre zorunludur")
        String password
) {
}
