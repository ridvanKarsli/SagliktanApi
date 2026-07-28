package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import com.ridvankarsli.sagliktanapi.service.ReactionSummary;

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
        long helpfulCount,
        long notHelpfulCount,
        ReactionValue myReaction,
        LocalDateTime createdAt,
        // Yanıtın yanıtları da dahil, sınırsız derinlikte gömülü olarak gelir
        // - bkz. buildTree.
        List<CommentResponse> replies
) {
    private static final String DELETED_PLACEHOLDER = "[Bu yorum silindi]";

    // Reaksiyon bilgisi olmayan yerler için (ör. yeni oluşturulan yorum) kısayol.
    public static CommentResponse from(Comment comment) {
        return build(comment, List.of(), ReactionSummary.empty());
    }

    public static CommentResponse from(Comment comment, ReactionSummary reactions) {
        return build(comment, List.of(), reactions);
    }

    // Bir postun üst-seviye yorumlarını, tüm alt yanıtlarıyla (her
    // derinlikten) birlikte tek seferde ağaca dönüştürür. allDescendants
    // postun tüm alt-seviye yorumlarını (parentCommentId dolu olanları)
    // flat halde içermeli - bkz. CommentService.listDescendants.
    // reactionsByCommentId: postun tüm yorumları için tek seferde toplu
    // çekilmiş reaksiyon özetleri (bkz. ReactionService.getSummaries) - her
    // yorum düğümü için ayrı sorgu atılmasını önler.
    public static List<CommentResponse> buildTree(
            List<Comment> topLevelComments, List<Comment> allDescendants, Map<Long, ReactionSummary> reactionsByCommentId
    ) {
        Map<Long, List<Comment>> byParentId = allDescendants.stream()
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));
        return topLevelComments.stream()
                .map(comment -> buildWithChildren(comment, byParentId, reactionsByCommentId))
                .toList();
    }

    public static CommentResponse buildWithChildren(
            Comment comment, Map<Long, List<Comment>> byParentId, Map<Long, ReactionSummary> reactionsByCommentId
    ) {
        List<Comment> children = byParentId.getOrDefault(comment.getId(), List.of());
        List<CommentResponse> childResponses = children.stream()
                .map(child -> buildWithChildren(child, byParentId, reactionsByCommentId))
                .toList();
        return build(comment, childResponses, reactionsByCommentId.getOrDefault(comment.getId(), ReactionSummary.empty()));
    }

    private static CommentResponse build(Comment comment, List<CommentResponse> replies, ReactionSummary reactions) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getFirstName() + " " + comment.getUser().getLastName(),
                comment.isDeleted() ? DELETED_PLACEHOLDER : comment.getContent(),
                comment.isDeleted(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                reactions.helpfulCount(),
                reactions.notHelpfulCount(),
                reactions.myReaction(),
                comment.getCreatedAt(),
                replies
        );
    }
}
