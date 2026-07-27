package com.ridvankarsli.sagliktanapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank String content,

        // null ise üst-seviye yorum, dolu ise bir yoruma verilen yanıt.
        Long parentCommentId
) {
}
