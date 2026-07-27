package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Post;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getSubGroup().getId(),
                post.getSubGroup().getDiseaseGroup().getId(),
                post.getUser().getId(),
                post.getUser().getFirstName() + " " + post.getUser().getLastName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
