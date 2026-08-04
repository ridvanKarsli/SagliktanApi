package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.PostAttachment;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;

// Faz 2 adım 4: PostAttachment entity'sinin dışa dönük hali - storage key
// gibi iç detaylar sızdırılmıyor, sadece frontend'in <img> src'ye
// koyabileceği tam public URL dönüyor.
public record PostAttachmentResponse(Long id, String url, int sortOrder) {

    public static PostAttachmentResponse from(PostAttachment attachment, MediaStorageService mediaStorageService) {
        return new PostAttachmentResponse(
                attachment.getId(),
                mediaStorageService.publicUrlFor(attachment.getStorageKey()),
                attachment.getSortOrder()
        );
    }
}
