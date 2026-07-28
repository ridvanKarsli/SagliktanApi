package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Post;

import java.time.LocalDateTime;

// Admin'in genel içerik moderasyonu ekranı için hafif DTO - reaksiyon
// özeti gibi burada gerekmeyen alanları taşımıyor (bkz. PostResponse, o
// kullanıcıya dönük uçlar için).
public record AdminPostResponse(
        Long id,
        Long subGroupId,
        Long diseaseGroupId,
        Long authorId,
        String authorName,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static AdminPostResponse from(Post post) {
        return new AdminPostResponse(
                post.getId(),
                post.getSubGroup().getId(),
                post.getSubGroup().getDiseaseGroup().getId(),
                post.getUser().getId(),
                post.getUser().getFirstName() + " " + post.getUser().getLastName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt()
        );
    }
}
