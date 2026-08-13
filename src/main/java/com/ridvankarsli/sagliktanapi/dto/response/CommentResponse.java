package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import com.ridvankarsli.sagliktanapi.service.ReactionSummary;

import java.time.LocalDateTime;

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
        // Basit içerik moderasyonu (bkz. ContentModerationService): true ise
        // frontend destekleyici bir kaynak bilgisi (182 ALO Yaşam Hattı)
        // gösterir - bu alan içeriği ASLA gizlemez/engellemez.
        boolean flaggedSensitive,
        LocalDateTime createdAt,
        // Bu yorumun DOĞRUDAN yanıt sayısı (alt yanıtların yanıtları dahil
        // değil). Eskiden burada tüm alt ağaç (sınırsız derinlik) gömülü
        // olarak geliyordu; artık talep üzerine GET /api/comments/{id}/replies
        // ile sayfalı çekiliyor (bkz. CommentService.listReplies) - bu sayede
        // çok yanıtlı bir yorumun tüm ağacını tek istekte belleğe/ağa çekme
        // sorunu ortadan kalkıyor.
        long replyCount
) {
    private static final String DELETED_PLACEHOLDER = "[Bu yorum silindi]";

    // Reaksiyon/yanıt sayısı bilgisi olmayan yerler için (ör. yeni
    // oluşturulan yorum - henüz hiç yanıtı yok) kısayol.
    public static CommentResponse from(Comment comment) {
        return from(comment, ReactionSummary.empty(), 0L);
    }

    public static CommentResponse from(Comment comment, ReactionSummary reactions) {
        return from(comment, reactions, 0L);
    }

    public static CommentResponse from(Comment comment, ReactionSummary reactions, long replyCount) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getFullName(),
                comment.isDeleted() ? DELETED_PLACEHOLDER : comment.getContent(),
                comment.isDeleted(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                reactions.helpfulCount(),
                reactions.notHelpfulCount(),
                reactions.myReaction(),
                comment.isFlaggedSensitive(),
                comment.getCreatedAt(),
                replyCount
        );
    }
}
