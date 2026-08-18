package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import com.ridvankarsli.sagliktanapi.service.ReactionSummary;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        Long subGroupId,
        // Frontend'in ekstra bir sub-group sorgusu atmadan "bu postun ait
        // olduğu hastalık grubuna üye miyim" kontrolü yapabilmesi için
        // (bkz. PostDetail.jsx - üye olmayan kullanıcıya yorum kutusu hiç
        // gösterilmiyor, backend zaten reddediyordu ama arayüz bunu
        // saklamıyordu).
        Long diseaseGroupId,
        // Ana sayfa karışık akışında (bkz. PostResponseAssembler.assembleFeed)
        // her gönderinin hangi alt/hastalık grubundan geldiğini göstermek
        // için dolduruluyor - tek bir alt grup bağlamında zaten bilinen diğer
        // tüm listelerde (Posts.jsx, arama, profil vb.) null kalıyor, gereksiz
        // veri taşınmasın diye.
        String subGroupName,
        String diseaseGroupName,
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
        // Faz 2 adım 4: gönderiye eklenmiş fotoğraflar, galeri sırasına
        // göre (bkz. PostAttachment.sortOrder).
        List<PostAttachmentResponse> attachments,
        // Basit içerik moderasyonu (bkz. ContentModerationService): true ise
        // frontend destekleyici bir kaynak bilgisi (182 ALO Yaşam Hattı)
        // gösterir - bu alan içeriği ASLA gizlemez/engellemez.
        boolean flaggedSensitive,
        // Faz6: sabitlenmiş gönderi (bkz. V20 migration + PostServiceImpl.pin).
        // Profil sayfaları (Profile.jsx/UserProfile.jsx) bu alanı görünce
        // gönderiyi "Gönderiler" sekmesinin en üstünde bir rozetle gösteriyor.
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Reaksiyon/kaydetme/fotoğraf bilgisi olmayan yerler için kısayol.
    public static PostResponse from(Post post) {
        return from(post, ReactionSummary.empty(), false, 0L, List.of());
    }

    public static PostResponse from(
            Post post, ReactionSummary reactions, boolean saved, long savedCount,
            List<PostAttachmentResponse> attachments
    ) {
        return from(post, reactions, saved, savedCount, attachments, null, null);
    }

    // Ana sayfa akışı için: alt grup/hastalık grubu ismini de dolduran hâli
    // (bkz. PostResponseAssembler.assembleFeed).
    public static PostResponse from(
            Post post, ReactionSummary reactions, boolean saved, long savedCount,
            List<PostAttachmentResponse> attachments, String subGroupName, String diseaseGroupName
    ) {
        return new PostResponse(
                post.getId(),
                post.getSubGroup().getId(),
                post.getSubGroup().getDiseaseGroup().getId(),
                subGroupName,
                diseaseGroupName,
                post.getUser().getId(),
                post.getUser().getFullName(),
                post.getTitle(),
                post.getContent(),
                reactions.helpfulCount(),
                reactions.notHelpfulCount(),
                reactions.myReaction(),
                saved,
                savedCount,
                attachments,
                post.isFlaggedSensitive(),
                post.isPinned(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
