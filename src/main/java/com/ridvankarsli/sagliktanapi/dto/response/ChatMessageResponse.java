package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Message;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.PostAttachment;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import com.ridvankarsli.sagliktanapi.service.PostAttachmentService;

import java.time.LocalDateTime;
import java.util.List;

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
        // Faz 2 adım 7: mesajla paylaşılan gönderi önizlemesi - post silinmişse
        // (shared_post_id NULL, bkz. V16 ON DELETE SET NULL) null döner,
        // frontend "gönderi silinmiş" durumunu buradan ayırt eder.
        SharedPostPreview sharedPost,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    private static final int CONTENT_SNIPPET_MAX_LENGTH = 160;

    public record SharedPostPreview(
            Long id, Long subGroupId, String title, String contentSnippet, String authorName, String thumbnailUrl
    ) {
    }

    public static ChatMessageResponse from(
            Message m, MediaStorageService mediaStorageService, PostAttachmentService postAttachmentService) {
        return new ChatMessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getContent(),
                m.getAttachmentKey() != null ? mediaStorageService.publicUrlFor(m.getAttachmentKey()) : null,
                buildSharedPostPreview(m.getSharedPost(), mediaStorageService, postAttachmentService),
                m.getReadAt(),
                m.getCreatedAt()
        );
    }

    private static SharedPostPreview buildSharedPostPreview(
            Post post, MediaStorageService mediaStorageService, PostAttachmentService postAttachmentService) {
        if (post == null) {
            return null;
        }
        List<PostAttachment> attachments = postAttachmentService.findByPostId(post.getId());
        String thumbnailUrl = attachments.isEmpty()
                ? null
                : mediaStorageService.publicUrlFor(attachments.get(0).getStorageKey());
        String content = post.getContent();
        String snippet = content != null && content.length() > CONTENT_SNIPPET_MAX_LENGTH
                ? content.substring(0, CONTENT_SNIPPET_MAX_LENGTH) + "…"
                : content;
        return new SharedPostPreview(
                post.getId(),
                post.getSubGroup().getId(),
                post.getTitle(),
                snippet,
                post.getUser().getFullName(),
                thumbnailUrl
        );
    }
}
