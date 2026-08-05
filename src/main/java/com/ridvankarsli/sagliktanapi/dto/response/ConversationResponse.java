package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.Conversation;
import com.ridvankarsli.sagliktanapi.domain.Message;
import com.ridvankarsli.sagliktanapi.domain.User;

import java.time.LocalDateTime;

// Sohbet listesi satırı: karşı taraf kim, son mesaj ne, kaç okunmamış mesaj
// var. lastMessage/unreadCount MessageController'da toplu (batch) çekilip
// buraya enjekte ediliyor - bkz. MessageRepository.findLastMessagesForConversations
// / countUnreadGrouped (N+1'den kaçınmak için, PostAttachment'takiyle aynı desen).
public record ConversationResponse(
        Long id,
        Long otherUserId,
        String otherUserName,
        String lastMessagePreview,
        boolean lastMessageHasAttachment,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
    public static ConversationResponse from(
            Conversation conversation, User otherUser, Message lastMessage, long unreadCount) {
        return new ConversationResponse(
                conversation.getId(),
                otherUser.getId(),
                otherUser.getFirstName() + " " + otherUser.getLastName(),
                lastMessage != null ? lastMessage.getContent() : null,
                lastMessage != null && lastMessage.getAttachmentKey() != null,
                lastMessage != null ? lastMessage.getCreatedAt() : conversation.getCreatedAt(),
                unreadCount
        );
    }
}
