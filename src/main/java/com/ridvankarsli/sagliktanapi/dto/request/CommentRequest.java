package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank String content
) {
}
