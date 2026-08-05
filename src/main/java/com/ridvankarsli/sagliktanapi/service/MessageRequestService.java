package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Conversation;
import com.ridvankarsli.sagliktanapi.domain.MessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Faz 2 adım 6: mesajlaşma öncesi onay akışı - biri karşı tarafa serbestçe
// mesaj atamaz, önce bir istek gönderir.
public interface MessageRequestService {

    // İnceliği: karşı taraf zaten SIZE bekleyen bir istek göndermişse (iki
    // kullanıcı birbirine aynı anda istek yollamış), yeni istek açmak
    // yerine o isteği otomatik kabul edip konuşmayı başlatıyoruz - bu
    // durumda Outcome.conversation dolu döner, aksi halde Outcome.request.
    Outcome send(Long senderId, Long recipientId);

    Page<MessageRequest> listIncoming(Long recipientId, Pageable pageable);

    long countPending(Long recipientId);

    Conversation accept(Long requestId, Long recipientId);

    void reject(Long requestId, Long recipientId);

    record Outcome(MessageRequest request, Conversation conversation) {
        public static Outcome ofRequest(MessageRequest request) {
            return new Outcome(request, null);
        }

        public static Outcome ofAutoAccepted(Conversation conversation) {
            return new Outcome(null, conversation);
        }

        public boolean autoAccepted() {
            return conversation != null;
        }
    }
}
