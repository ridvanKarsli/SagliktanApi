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
        // Faz 2 adım 3: istek sahibi bu gönderiyi kaydetmiş (yıldızlamış) mi.
        boolean saved,
        // Faz 2 adım 3b: bu gönderiyi toplam kaç kişi kaydetmiş - popülerlik
        // sıralamasına da dahil ediliyor (bkz. PostRepository.
        // findBySubGroupIdOrderByPopularityDesc).
        long savedCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Reaksiyon/kaydetme bilgisi olmayan yerler için (ör. yeni oluşturulan
    // post - henüz kimse reaksiyon veremedi/kaydedemedi) kısayol.
    public static PostResponse from(Post post) {
        return from(post, ReactionSummary.empty(), false, 0L);
    }

    public static PostResponse from(Post post, ReactionSummary reactions, boolean saved, long savedCount) {
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
                saved,
                savedCount,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
