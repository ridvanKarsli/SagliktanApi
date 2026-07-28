package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import com.ridvankarsli.sagliktanapi.service.ReactionSummary;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        Long subGroupId,
        // Frontend'in ekstra bir sub-group sorgusu atmadan "bu postun ait
        // olduğu hastalık grubuna üye miyim" kontrolü yapabilmesi için
        // (bkz. PostDetail.jsx - üye olmayan kullanıcıya yorum kutusu hiç
        // gösterilmiyor, backend zaten reddediyordu ama arayüz bunu
        // saklamıyordu).
        Long diseaseGroupId,
        Long authorId,
        String authorName,
        String title,
        String content,
        long helpfulCount,
        long notHelpfulCount,
        // null ise istek sahibi bu posta hiç reaksiyon vermemiş.
        ReactionValue myReaction,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Reaksiyon bilgisi olmayan yerler için (ör. yeni oluşturulan post -
    // henüz kimse reaksiyon veremedi) kısayol.
    public static PostResponse from(Post post) {
        return from(post, ReactionSummary.empty());
    }

    public static PostResponse from(Post post, ReactionSummary reactions) {
        return new PostResponse(
                post.getId(),
                post.getSubGroup().getId(),
                post.getSubGroup().getDiseaseGroup().getId(),
                post.getUser().getId(),
                post.getUser().getFirstName() + " " + post.getUser().getLastName(),
                post.getTitle(),
                post.getContent(),
                reactions.helpfulCount(),
                reactions.notHelpfulCount(),
                reactions.myReaction(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
