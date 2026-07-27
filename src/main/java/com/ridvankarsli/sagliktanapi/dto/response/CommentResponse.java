package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        // null ise üst-seviye yorum, dolu ise bu bir yanıttır (parent'ın id'si).
        Long parentCommentId,
        LocalDateTime createdAt,
        // Sadece üst-seviye yorumlarda dolu gelir (bkz. CommentController) -
        // yanıtların kendi yanıtları olamaz, derinlik tek seviyeyle sınırlı.
        List<CommentResponse> replies
) {
    // Yanıtı olmayan / henüz yeni oluşturulmuş bir yorum için kısayol.
    public static CommentResponse from(Comment comment) {
        return from(comment, List.of());
    }

    public static CommentResponse from(Comment comment, List<Comment> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getFirstName() + " " + comment.getUser().getLastName(),
                comment.getContent(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getCreatedAt(),
                replies.stream().map(CommentResponse::from).toList()
        );
    }
}
