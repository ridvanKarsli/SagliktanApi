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
        // Faz 2 adım 7: son mesaj bir gönderi paylaşımıysa (metin/fotoğraf
        // içermeyen, sadece sharedPost'lu bir mesajsa) sohbet listesinde
        // boş önizleme yerine bunu göstermek için (bkz. Conversations.jsx).
        boolean lastMessageHasSharedPost,
        LocalDateTime lastMessageAt,
        long unreadCount,
        // İki yönde de engel yoksa true. Kimin kimi engellediğini AYIRT
        // ETMEZ - o bilgi zaten kullanıcının kendi engelli listesinden
        // (bkz. GET /messages/blocked) çıkarılabiliyor; ikisi birlikte
        // Chat.jsx'te "sen engelledin" / "o seni engellemiş" ayrımını yapar.
        boolean canMessage
) {
    public static ConversationResponse from(
            Conversation conversation, User otherUser, Message lastMessage, long unreadCount, boolean canMessage) {
        return new ConversationResponse(
                conversation.getId(),
                otherUser.getId(),
                otherUser.getFirstName() + " " + otherUser.getLastName(),
                lastMessage != null ? lastMessage.getContent() : null,
                lastMessage != null && lastMessage.getAttachmentKey() != null,
                lastMessage != null && lastMessage.getSharedPost() != null,
                lastMessage != null ? lastMessage.getCreatedAt() : conversation.getCreatedAt(),
                unreadCount,
                canMessage
        );
    }
}
