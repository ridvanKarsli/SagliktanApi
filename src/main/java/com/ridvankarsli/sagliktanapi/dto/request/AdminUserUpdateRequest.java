package com.ridvankarsli.sagliktanapi.dto.request;

import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.validation.ValidName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @NotBlank(message = "Ad zorunludur")
        @ValidName
        String firstName,

        @NotBlank(message = "Soyad zorunludur")
        @ValidName
        String lastName,

        @Size(max = 1000, message = "Biyografi en fazla 1000 karakter olabilir")
        String bio,

        @NotNull(message = "Rol zorunludur")
        Role role,

        boolean active
) {
}
