package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Message;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;

import java.time.LocalDateTime;

// Faz 2 adım 6: bir konuşma içindeki tek mesajın dışa dönük hali. Adı
// bilerek "ChatMessageResponse" - dto.response.MessageResponse zaten var
// olan, ilişkisiz bir genel "başarı mesajı" wrapper'ı (verify-email,
// logout vb. için), isim çakışmasını önlemek için buna dokunulmadı.
// PostAttachmentResponse ile aynı desen: storageKey sızmıyor, frontend'in
// doğrudan kullanabileceği tam public URL dönüyor.
public record ChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        String attachmentUrl,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(Message m, MediaStorageService mediaStorageService) {
        return new ChatMessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getContent(),
                m.getAttachmentKey() != null ? mediaStorageService.publicUrlFor(m.getAttachmentKey()) : null,
                m.getReadAt(),
                m.getCreatedAt()
        );
    }
}
