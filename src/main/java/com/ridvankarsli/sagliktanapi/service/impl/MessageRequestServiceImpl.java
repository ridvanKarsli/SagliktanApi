package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Conversation;
import com.ridvankarsli.sagliktanapi.domain.MessageRequest;
import com.ridvankarsli.sagliktanapi.domain.MessageRequestStatus;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.response.MessageRequestResponse;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.MessageRequestRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.BlockService;
import com.ridvankarsli.sagliktanapi.service.ConversationService;
import com.ridvankarsli.sagliktanapi.service.MessageRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRequestServiceImpl implements MessageRequestService {

    private final MessageRequestRepository messageRequestRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final BlockService blockService;
    // WebSocket bidirectional gereksinimi: NotificationServiceImpl'deki gibi
    // "istemci REST ile gönderir, sunucu WS ile anlık push eder" deseni -
    // ayrı bir @MessageMapping/STOMP inbound controller'a gerek yok (bkz.
    // PLAN_faz2_ozellikler.md adım 6'daki not: "daha az değişiklik").
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Outcome send(Long senderId, Long recipientId) {
        if (senderId.equals(recipientId)) {
            throw new BadRequestException("Kendinize mesaj isteği gönderemezsiniz");
        }
        blockService.assertNotBlocked(senderId, recipientId);

        // Aramızda zaten kabul edilmiş bir konuşma varsa yeni istek gereksiz.
        Optional<Conversation> existingConversation = conversationService.findExisting(senderId, recipientId);
        if (existingConversation.isPresent()) {
            return Outcome.ofAutoAccepted(existingConversation.get());
        }

        // Karşı taraf bize zaten istek göndermişse (iki kullanıcı aynı anda
        // birbirine istek yollamış), yeni istek açmak yerine onu kabul edip
        // konuşmayı başlatıyoruz.
        Optional<MessageRequest> reverse = messageRequestRepository
                .findBySenderIdAndRecipientIdAndStatus(recipientId, senderId, MessageRequestStatus.PENDING);
        if (reverse.isPresent()) {
            return Outcome.ofAutoAccepted(acceptInternal(reverse.get()));
        }

        // Bizim zaten bekleyen isteğimiz varsa (uq_message_requests_pending
        // constraint'i de bunu engeller) tekrar oluşturmak yerine var olanı
        // dönüyoruz - kullanıcıya 500 yerine tutarlı bir sonuç.
        Optional<MessageRequest> existing = messageRequestRepository
                .findBySenderIdAndRecipientIdAndStatus(senderId, recipientId, MessageRequestStatus.PENDING);
        if (existing.isPresent()) {
            return Outcome.ofRequest(existing.get());
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        MessageRequest request = messageRequestRepository.save(
                MessageRequest.builder().sender(sender).recipient(recipient).build());

        pushNewRequest(request);
        return Outcome.ofRequest(request);
    }

    @Override
    public Page<MessageRequest> listIncoming(Long recipientId, Pageable pageable) {
        return messageRequestRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(
                recipientId, MessageRequestStatus.PENDING, pageable);
    }

    @Override
    public Page<MessageRequest> listOutgoing(Long senderId, Pageable pageable) {
        return messageRequestRepository.findBySenderIdAndStatusOrderByCreatedAtDesc(
                senderId, MessageRequestStatus.PENDING, pageable);
    }

    @Override
    public long countPending(Long recipientId) {
        return messageRequestRepository.countByRecipientIdAndStatus(recipientId, MessageRequestStatus.PENDING);
    }

    @Override
    @Transactional
    public Conversation accept(Long requestId, Long recipientId) {
        return acceptInternal(getOwnedPendingRequest(requestId, recipientId));
    }

    @Override
    @Transactional
    public void reject(Long requestId, Long recipientId) {
        MessageRequest request = getOwnedPendingRequest(requestId, recipientId);
        request.setStatus(MessageRequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        messageRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void cancel(Long requestId, Long senderId) {
        MessageRequest request = messageRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesaj isteği bulunamadı"));
        if (!request.getSender().getId().equals(senderId)) {
            throw new ForbiddenException("Bu istek size ait değil");
        }
        if (request.getStatus() != MessageRequestStatus.PENDING) {
            throw new BadRequestException("Bu istek zaten yanıtlanmış");
        }
        messageRequestRepository.delete(request);
    }

    // Sadece alıcı kendi bekleyen isteğini kabul/red edebilir - gönderen
    // kendi isteğini "kabul" edemez.
    private MessageRequest getOwnedPendingRequest(Long requestId, Long recipientId) {
        MessageRequest request = messageRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesaj isteği bulunamadı"));
        if (!request.getRecipient().getId().equals(recipientId)) {
            throw new ForbiddenException("Bu istek size ait değil");
        }
        if (request.getStatus() != MessageRequestStatus.PENDING) {
            throw new BadRequestException("Bu istek zaten yanıtlanmış");
        }
        return request;
    }

    private Conversation acceptInternal(MessageRequest request) {
        request.setStatus(MessageRequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        messageRequestRepository.save(request);
        return conversationService.getOrCreateBetween(
                request.getSender().getId(), request.getRecipient().getId());
    }

    // NotificationServiceImpl.notifyIfNotSelf'teki aynı gerekçe: convertAndSendToUser
    // async'tir, alıcı bağlı değilse mesaj sessizce düşer (REST fallback:
    // MessageController.listIncoming); burada yakalanan hata sadece erken/senkron hatalar.
    private void pushNewRequest(MessageRequest request) {
        try {
            messagingTemplate.convertAndSendToUser(
                    request.getRecipient().getEmail(),
                    "/queue/message-requests",
                    MessageRequestResponse.from(request));
        } catch (RuntimeException e) {
            log.error("Mesaj isteği WS push başarısız: recipient={}, requestId={}",
                    request.getRecipient().getEmail(), request.getId(), e);
        }
    }
}
