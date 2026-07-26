package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubGroupRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description
) {
}
