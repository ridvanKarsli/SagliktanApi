package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        Long subGroupId,
        Long authorId,
        String authorName,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getSubGroup().getId(),
                post.getUser().getId(),
                post.getUser().getFirstName() + " " + post.getUser().getLastName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
