package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Conversation;
import com.ridvankarsli.sagliktanapi.domain.Message;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.response.ChatMessageResponse;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.MessageRepository;
import com.ridvankarsli.sagliktanapi.service.BlockService;
import com.ridvankarsli.sagliktanapi.service.ConversationService;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import com.ridvankarsli.sagliktanapi.service.MessageService;
import com.ridvankarsli.sagliktanapi.util.MediaConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final BlockService blockService;
    private final MediaStorageService mediaStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Message send(Long conversationId, Long senderId, String content, String attachmentKey) {
        boolean hasContent = content != null && !content.isBlank();
        boolean hasAttachment = attachmentKey != null && !attachmentKey.isBlank();
        if (!hasContent && !hasAttachment) {
            throw new BadRequestException("Mesaj boş olamaz");
        }

        Conversation conversation = conversationService.getById(conversationId);
        conversationService.assertParticipant(conversation, senderId);

        User other = conversationService.otherParticipant(conversation, senderId);
        // Konuşma kabul edilmiş bir istekten doğduğu için başlangıçta engel
        // yoktur, ama taraflar mesajlaştıktan SONRA da birbirini
        // engelleyebilir - bu yüzden her mesajda tekrar kontrol ediliyor.
        blockService.assertNotBlocked(senderId, other.getId());

        if (hasAttachment) {
            validateAttachment(attachmentKey);
        }

        User sender = conversation.getUserOne().getId().equals(senderId)
                ? conversation.getUserOne()
                : conversation.getUserTwo();

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(hasContent ? content : null)
                .attachmentKey(hasAttachment ? attachmentKey : null)
                .build());

        pushNewMessage(message, other);
        return message;
    }

    @Override
    public Page<Message> list(Long conversationId, Long requesterId, Pageable pageable) {
        Conversation conversation = conversationService.getById(conversationId);
        conversationService.assertParticipant(conversation, requesterId);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }

    @Override
    @Transactional
    public void markRead(Long conversationId, Long readerId) {
        Conversation conversation = conversationService.getById(conversationId);
        conversationService.assertParticipant(conversation, readerId);
        messageRepository.markConversationRead(conversationId, readerId);
    }

    @Override
    public long countUnread(Long userId) {
        return messageRepository.countUnreadForUser(userId);
    }

    @Override
    @Transactional
    public void deleteAsAdmin(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesaj bulunamadı"));
        if (message.getAttachmentKey() != null) {
            mediaStorageService.deleteObjects(List.of(message.getAttachmentKey()));
        }
        messageRepository.delete(message);
    }

    // PostAttachmentServiceImpl.attach'teki tekli doğrulamayla aynı kural
    // seti (MediaConstraints'ten paylaşılan sabitler) - mesaj fotoğrafı için
    // ayrı bir limit tanımlanmadı, tek fotoğraf olduğu için "adet" kontrolüne
    // gerek yok.
    private void validateAttachment(String attachmentKey) {
        MediaStorageService.ObjectMetadata metadata = mediaStorageService.headObject(attachmentKey)
                .orElseThrow(() -> new BadRequestException("Yüklenen fotoğraf bulunamadı: " + attachmentKey));
        if (!MediaConstraints.isAllowedContentType(metadata.contentType())) {
            mediaStorageService.deleteObjects(List.of(attachmentKey));
            throw new BadRequestException("Desteklenmeyen dosya tipi: " + metadata.contentType());
        }
        if (metadata.contentLength() > MediaConstraints.MAX_FILE_SIZE_BYTES) {
            mediaStorageService.deleteObjects(List.of(attachmentKey));
            throw new BadRequestException("Dosya çok büyük (maksimum "
                    + (MediaConstraints.MAX_FILE_SIZE_BYTES / (1024 * 1024)) + "MB)");
        }
    }

    // NotificationServiceImpl ile aynı desen: convertAndSendToUser async'tir,
    // alıcı bağlı değilse mesaj sessizce düşer (REST fallback: MessageController.list
    // konuşma açıldığında zaten tüm geçmişi çeker), burada yakalanan hata
    // sadece erken/senkron hatalar.
    private void pushNewMessage(Message message, User recipient) {
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/messages",
                    ChatMessageResponse.from(message, mediaStorageService));
        } catch (RuntimeException e) {
            log.error("Mesaj WS push başarısız: recipient={}, messageId={}",
                    recipient.getEmail(), message.getId(), e);
        }
    }
}
