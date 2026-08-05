package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Comment;

import java.time.LocalDateTime;

public record AdminCommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        boolean deleted,
        LocalDateTime createdAt
) {
    public static AdminCommentResponse from(Comment comment) {
        return new AdminCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getFullName(),
                comment.getContent(),
                comment.isDeleted(),
                comment.getCreatedAt()
        );
    }
}
