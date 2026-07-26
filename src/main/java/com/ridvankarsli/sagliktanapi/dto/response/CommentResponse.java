package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getFirstName() + " " + comment.getUser().getLastName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
