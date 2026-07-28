package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // Yeni oluşturulan bir yorumdan yola çıkarak (üst-seviye ise post
    // sahibine, yanıt ise üst yorumun sahibine) bildirim üretir ve
    // WebSocket üzerinden anlık push eder. Kendi içeriğine kendi aksiyonu
    // bildirim üretmez (bkz. NotificationServiceImpl).
    void notifyNewComment(Comment comment);

    Page<Notification> list(Long recipientId, Pageable pageable);

    long countUnread(Long recipientId);

    // Sadece bildirimin sahibi okundu işaretleyebilir - bkz. ForbiddenException.
    void markRead(Long notificationId, Long requesterId);

    void markAllRead(Long recipientId);
}
