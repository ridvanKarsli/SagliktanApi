package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Conversation;
import com.ridvankarsli.sagliktanapi.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

// Faz 2 adım 6: iki kullanıcı arasındaki tek konuşmayı yönetir. user1/user2
// parametrelerinin sırası önemli değil - içeride her zaman küçük id/büyük id
// olarak canonicalize edilir (bkz. Conversation.java javadoc), böylece A-B
// ve B-A için yanlışlıkla iki ayrı konuşma açılmaz.
public interface ConversationService {

    Optional<Conversation> findExisting(Long user1Id, Long user2Id);

    // Konuşma yoksa oluşturur (MessageRequestService.accept çağırır), varsa
    // olduğu gibi döner (idempotent).
    Conversation getOrCreateBetween(Long user1Id, Long user2Id);

    Conversation getById(Long conversationId);

    // requesterId bu konuşmanın tarafı değilse ForbiddenException.
    void assertParticipant(Conversation conversation, Long requesterId);

    Page<Conversation> listForUser(Long userId, Pageable pageable);

    // Konuşmadaki "diğer" kullanıcı - MessageResponse/ConversationResponse
    // oluştururken karşı tarafın kim olduğunu bulmak için.
    User otherParticipant(Conversation conversation, Long userId);
}
