package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.Notification;
import com.ridvankarsli.sagliktanapi.domain.NotificationType;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.response.NotificationResponse;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.NotificationRepository;
import com.ridvankarsli.sagliktanapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void notifyNewComment(Comment comment) {
        User actor = comment.getUser();

        if (comment.getParentComment() != null) {
            // Bu bir yanıt: üst yorumun sahibine bildirim git.
            notifyIfNotSelf(comment.getParentComment().getUser(), actor, NotificationType.COMMENT_REPLY, comment);
        } else {
            // Bu üst-seviye bir yorum: postun sahibine bildirim git.
            notifyIfNotSelf(comment.getPost().getUser(), actor, NotificationType.NEW_COMMENT, comment);
        }
    }

    // Kendi içeriğine kendi yorumun/yanıtın bildirim üretmesin diye kontrol.
    private void notifyIfNotSelf(User recipient, User actor, NotificationType type, Comment comment) {
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .postId(comment.getPost().getId())
                .commentId(comment.getId())
                .build();

        notification = notificationRepository.save(notification);

        // convertAndSendToUser, hedefi Principal.getName() (bkz.
        // JwtHandshakeChannelInterceptor - bu değer kullanıcının e-postası)
        // ile eşleştirip /user/{email}/queue/notifications'a yönlendirir.
        // Alıcı o an bağlı değilse mesaj sessizce düşer - bu yüzden ayrıca
        // REST fallback uçları var (bkz. NotificationController).
        //
        // NOT: convertAndSendToUser mesajı clientOutboundChannel'a (varsayılan
        // olarak async bir ThreadPoolTaskExecutor) bırakır - gerçek teslimat bu
        // metod döndükten SONRA, başka bir thread'de gerçekleşir. Burada
        // yakalanan hata sadece senkron/erken hataları kapsar.
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(), "/queue/notifications", NotificationResponse.from(notification));
        } catch (RuntimeException e) {
            log.error("Bildirim WS push başarısız: recipient={}, notificationId={}",
                    recipient.getEmail(), notification.getId(), e);
        }
    }

    @Override
    public Page<Notification> list(Long recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
    }

    @Override
    public long countUnread(Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim bulunamadı"));

        if (!notification.getRecipient().getId().equals(requesterId)) {
            throw new ForbiddenException("Bu bildirim size ait değil");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllReadForRecipient(recipientId);
    }
}
