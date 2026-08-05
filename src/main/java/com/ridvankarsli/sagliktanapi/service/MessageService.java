package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Faz 2 adım 6: kabul edilmiş bir konuşma içindeki mesajlar. Konuşmanın
// kendisi (kimlerin arasında olduğu, oluşturulması) ConversationService'in
// işi - burası sadece o konuşma içindeki mesaj akışını yönetir.
public interface MessageService {

    // conversationId'nin senderId'nin gerçekten tarafı olduğu, karşı tarafın
    // engellenmediği ve content/attachmentKey'den en az birinin dolu olduğu
    // burada doğrulanır.
    Message send(Long conversationId, Long senderId, String content, String attachmentKey);

    Page<Message> list(Long conversationId, Long requesterId, Pageable pageable);

    void markRead(Long conversationId, Long readerId);

    // Şikayet üzerinden admin moderasyonu için (bkz. AdminServiceImpl.resolveReport)
    // - sahiplik kontrolü YAPMAZ, çağıran taraf zaten admin yetkisini
    // garanti etmiş olmalı (AdminController'daki @PreAuthorize("hasRole('ADMIN')")
    // gibi). Fotoğraf ekliyse R2'deki dosya da temizlenir.
    void deleteAsAdmin(Long messageId);
}
