package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.dto.response.NotificationResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UnreadCountResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// WebSocket bağlantısı koptuğunda / ilk sayfa yüklemesinde senkronizasyon
// için REST fallback (bkz. NotificationServiceImpl - anlık push ayrıca
// /user/queue/notifications üzerinden yapılıyor).
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal CustomUserDetails principal, Pageable pageable
    ) {
        return PageResponse.from(
                notificationService.list(principal.getId(), pageable).map(NotificationResponse::from)
        );
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return new UnreadCountResponse(notificationService.countUnread(principal.getId()));
    }

    @PutMapping("/{id}/read")
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markRead(id, principal.getId());
    }

    @PutMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllRead(principal.getId());
    }
}
