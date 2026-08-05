package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.BlockedUser;
import com.ridvankarsli.sagliktanapi.domain.Conversation;
import com.ridvankarsli.sagliktanapi.domain.Message;
import com.ridvankarsli.sagliktanapi.domain.MessageRequest;
import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.request.MessageRequestCreateRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ReportRequest;
import com.ridvankarsli.sagliktanapi.dto.request.SendMessageRequest;
import com.ridvankarsli.sagliktanapi.dto.response.BlockedUserResponse;
import com.ridvankarsli.sagliktanapi.dto.response.ChatMessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.ConversationResponse;
import com.ridvankarsli.sagliktanapi.dto.response.MessageRequestResponse;
import com.ridvankarsli.sagliktanapi.dto.response.MessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UnreadCountResponse;
import com.ridvankarsli.sagliktanapi.repository.MessageRepository;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.BlockService;
import com.ridvankarsli.sagliktanapi.service.ContentReportService;
import com.ridvankarsli.sagliktanapi.service.ConversationService;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import com.ridvankarsli.sagliktanapi.service.MessageRequestService;
import com.ridvankarsli.sagliktanapi.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Faz 2 adım 6: birebir mesajlaşma. Üç alt kaynak tek controller'da
// toplanıyor (requests/conversations/block) - NotificationController'ın
// aksine burada tek bir domain kavramı (mesajlaşma) etrafında birden fazla
// ilişkili işlem var, ayrı controller'lara bölmek gereksiz parçalanma
// olurdu.
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRequestService messageRequestService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final BlockService blockService;
    private final ContentReportService contentReportService;
    private final MediaStorageService mediaStorageService;
    // Sohbet listesinde son mesaj/okunmamış sayısı toplu çekimi için -
    // MessageService'in genel sözleşmesine (tek konuşma bazlı) eklemek
    // yerine burada, sadece bu listeleme senaryosunda kullanılıyor.
    private final MessageRepository messageRepository;

    // --- Mesaj istekleri ---

    @PostMapping("/requests")
    public Map<String, Object> sendRequest(
            @Valid @RequestBody MessageRequestCreateRequest body, @AuthenticationPrincipal CustomUserDetails principal
    ) {
        MessageRequestService.Outcome outcome = messageRequestService.send(principal.getId(), body.recipientId());
        if (outcome.autoAccepted()) {
            return Map.of("autoAccepted", true, "conversationId", outcome.conversation().getId());
        }
        return Map.of("autoAccepted", false, "request", MessageRequestResponse.from(outcome.request()));
    }

    @GetMapping("/requests")
    public PageResponse<MessageRequestResponse> listIncomingRequests(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MessageRequest> page = messageRequestService.listIncoming(principal.getId(), pageable);
        return PageResponse.from(page.map(MessageRequestResponse::from));
    }

    @GetMapping("/requests/count")
    public UnreadCountResponse countPendingRequests(@AuthenticationPrincipal CustomUserDetails principal) {
        return new UnreadCountResponse(messageRequestService.countPending(principal.getId()));
    }

    // Kullanıcının kendi gönderdiği, hâlâ yanıt bekleyen istekler - "Giden
    // istekler" sekmesi.
    @GetMapping("/requests/outgoing")
    public PageResponse<MessageRequestResponse> listOutgoingRequests(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MessageRequest> page = messageRequestService.listOutgoing(principal.getId(), pageable);
        return PageResponse.from(page.map(MessageRequestResponse::from));
    }

    @DeleteMapping("/requests/{id}")
    public void cancelRequest(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        messageRequestService.cancel(id, principal.getId());
    }

    @PutMapping("/requests/{id}/accept")
    public Map<String, Long> acceptRequest(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Conversation conversation = messageRequestService.accept(id, principal.getId());
        return Map.of("conversationId", conversation.getId());
    }

    @PutMapping("/requests/{id}/reject")
    public void rejectRequest(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        messageRequestService.reject(id, principal.getId());
    }

    // --- Konuşmalar ve mesajlar ---

    // Sohbet ekranına doğrudan (ör. sayfa yenileme, paylaşılan link) girildiğinde
    // liste sayfasından geçmeden karşı tarafın kim olduğunu gösterebilmek için -
    // listConversations'taki toplu çekim burada gereksiz, tek konuşma için
    // doğrudan sorgulanıyor.
    @GetMapping("/conversations/{id}")
    public ConversationResponse getConversation(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Conversation conversation = conversationService.getById(id);
        conversationService.assertParticipant(conversation, principal.getId());
        User other = conversationService.otherParticipant(conversation, principal.getId());
        long unread = messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(id, principal.getId());
        return ConversationResponse.from(conversation, other, null, unread);
    }

    @GetMapping("/conversations")
    public PageResponse<ConversationResponse> listConversations(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault Pageable pageable
    ) {
        Page<Conversation> page = conversationService.listForUser(principal.getId(), pageable);
        List<Long> conversationIds = page.getContent().stream().map(Conversation::getId).toList();

        // "IN ()" boş koleksiyonla SQL hatası verir - sayfa boşsa (ör. hiç
        // konuşması olmayan kullanıcı) sorguları hiç atmıyoruz.
        Map<Long, Message> lastMessageByConversation = Map.of();
        Map<Long, Long> unreadByConversation = Map.of();
        if (!conversationIds.isEmpty()) {
            // N+1 yerine toplu çekim - PostAttachmentService.findByPostIds ile
            // aynı gerekçe/desen.
            lastMessageByConversation = messageRepository
                    .findLastMessagesForConversations(conversationIds).stream()
                    .collect(Collectors.toMap(m -> m.getConversation().getId(), m -> m));
            unreadByConversation = messageRepository
                    .countUnreadGrouped(conversationIds, principal.getId()).stream()
                    .collect(Collectors.toMap(
                            MessageRepository.UnreadCountRow::getConversationId, MessageRepository.UnreadCountRow::getCount));
        }
        Map<Long, Message> finalLastMessageByConversation = lastMessageByConversation;
        Map<Long, Long> finalUnreadByConversation = unreadByConversation;

        return PageResponse.from(page.map(conversation -> {
            User other = conversationService.otherParticipant(conversation, principal.getId());
            return ConversationResponse.from(
                    conversation,
                    other,
                    finalLastMessageByConversation.get(conversation.getId()),
                    finalUnreadByConversation.getOrDefault(conversation.getId(), 0L));
        }));
    }

    // Nav rozeti: tüm konuşmalardaki toplam okunmamış mesaj sayısı.
    @GetMapping("/unread-count")
    public UnreadCountResponse countUnreadMessages(@AuthenticationPrincipal CustomUserDetails principal) {
        return new UnreadCountResponse(messageService.countUnread(principal.getId()));
    }

    @GetMapping("/conversations/{id}/messages")
    public PageResponse<ChatMessageResponse> listMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Message> page = messageService.list(id, principal.getId(), pageable);
        return PageResponse.from(page.map(m -> ChatMessageResponse.from(m, mediaStorageService)));
    }

    @PostMapping("/conversations/{id}/messages")
    public ChatMessageResponse sendMessage(
            @PathVariable Long id, @RequestBody SendMessageRequest body, @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Message message = messageService.send(id, principal.getId(), body.content(), body.attachmentKey());
        return ChatMessageResponse.from(message, mediaStorageService);
    }

    @PutMapping("/conversations/{id}/read")
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        messageService.markRead(id, principal.getId());
    }

    // PostController/CommentController'daki /{id}/report uçlarıyla aynı
    // desen - bkz. ContentReportServiceImpl.assertTargetExists (MESSAGE dalı).
    @PostMapping("/{messageId}/report")
    public MessageResponse reportMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody(required = false) ReportRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        contentReportService.report(ReportTargetType.MESSAGE, messageId, principal.getId(), reason);
        return new MessageResponse("Şikayetiniz alındı, teşekkür ederiz");
    }

    // --- Engelleme ---

    @PostMapping("/block/{userId}")
    public void block(@PathVariable Long userId, @AuthenticationPrincipal CustomUserDetails principal) {
        blockService.block(principal.getId(), userId);
    }

    @DeleteMapping("/block/{userId}")
    public void unblock(@PathVariable Long userId, @AuthenticationPrincipal CustomUserDetails principal) {
        blockService.unblock(principal.getId(), userId);
    }

    @GetMapping("/blocked")
    public List<BlockedUserResponse> listBlocked(@AuthenticationPrincipal CustomUserDetails principal) {
        List<BlockedUser> blocked = blockService.listBlocked(principal.getId());
        return blocked.stream().map(BlockedUserResponse::from).toList();
    }
}
