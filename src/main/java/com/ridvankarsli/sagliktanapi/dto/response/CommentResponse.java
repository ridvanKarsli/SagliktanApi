package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        boolean deleted,
        // null ise üst-seviye yorum, dolu ise bu bir yanıttır (parent'ın id'si).
        Long parentCommentId,
        LocalDateTime createdAt,
        // Yanıtın yanıtları da dahil, sınırsız derinlikte gömülü olarak gelir
        // - bkz. buildTree.
        List<CommentResponse> replies
) {
    private static final String DELETED_PLACEHOLDER = "[Bu yorum silindi]";

    // Yanıtı olmayan / henüz yeni oluşturulmuş bir yorum için kısayol.
    public static CommentResponse from(Comment comment) {
        return build(comment, List.of());
    }

    // Bir postun üst-seviye yorumlarını, tüm alt yanıtlarıyla (her
    // derinlikten) birlikte tek seferde ağaca dönüştürür. allDescendants
    // postun tüm alt-seviye yorumlarını (parentCommentId dolu olanları)
    // flat halde içermeli - bkz. CommentService.listDescendants.
    public static List<CommentResponse> buildTree(List<Comment> topLevelComments, List<Comment> allDescendants) {
        Map<Long, List<Comment>> byParentId = allDescendants.stream()
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));
        return topLevelComments.stream()
                .map(comment -> buildWithChildren(comment, byParentId))
                .toList();
    }

    public static CommentResponse buildWithChildren(Comment comment, Map<Long, List<Comment>> byParentId) {
        List<Comment> children = byParentId.getOrDefault(comment.getId(), List.of());
        List<CommentResponse> childResponses = children.stream()
                .map(child -> buildWithChildren(child, byParentId))
                .toList();
        return build(comment, childResponses);
    }

    private static CommentResponse build(Comment comment, List<CommentResponse> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getFirstName() + " " + comment.getUser().getLastName(),
                comment.isDeleted() ? DELETED_PLACEHOLDER : comment.getContent(),
                comment.isDeleted(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getCreatedAt(),
                replies
        );
    }
}
