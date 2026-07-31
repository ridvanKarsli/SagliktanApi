package com.ridvankarsli.sagliktanapi.dto.request;

import com.ridvankarsli.sagliktanapi.validation.ValidName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @ValidName String firstName,
        @NotBlank @ValidName String lastName,
        @Size(max = 1000) String bio
) {
}
