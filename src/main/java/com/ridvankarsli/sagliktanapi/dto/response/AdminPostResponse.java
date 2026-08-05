package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Post;

import java.time.LocalDateTime;
import java.util.List;

// Admin'in genel içerik moderasyonu ekranı için hafif DTO - reaksiyon
// özeti gibi burada gerekmeyen alanları taşımıyor (bkz. PostResponse, o
// kullanıcıya dönük uçlar için). attachments (Faz 2 admin-moderasyon
// eklentisi) admin'in görsel içeriği doğrudan panelde görüp
// tehlikeli/uygunsuz olanları silebilmesi için var.
public record AdminPostResponse(
        Long id,
        Long subGroupId,
        Long diseaseGroupId,
        Long authorId,
        String authorName,
        String title,
        String content,
        List<PostAttachmentResponse> attachments,
        LocalDateTime createdAt
) {
    public static AdminPostResponse from(Post post, List<PostAttachmentResponse> attachments) {
        return new AdminPostResponse(
                post.getId(),
                post.getSubGroup().getId(),
                post.getSubGroup().getDiseaseGroup().getId(),
                post.getUser().getId(),
                post.getUser().getFullName(),
                post.getTitle(),
                post.getContent(),
                attachments,
                post.getCreatedAt()
        );
    }
}
