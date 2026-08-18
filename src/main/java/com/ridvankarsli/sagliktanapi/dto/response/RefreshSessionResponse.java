package com.ridvankarsli.sagliktanapi.dto.response;

import com.ridvankarsli.sagliktanapi.domain.RefreshSession;

import java.time.LocalDateTime;

// "Aktif Oturumlar" (görev #305/#306): Profile > Ayarlar altında kullanıcının
// kendi cihaz/oturum listesini görebilmesi için. Ham sessionId (JWT "sid")
// istemciye SIZDIRILMAZ - id sadece bu RefreshSession satırının DB PK'ı,
// revoke isteği bununla yapılır (bkz. AuthController/AuthServiceImpl.revokeSession).
public record RefreshSessionResponse(
        Long id,
        String deviceLabel,
        String ipAddress,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        boolean current
) {
    public static RefreshSessionResponse from(RefreshSession session, String currentSessionId) {
        return new RefreshSessionResponse(
                session.getId(),
                session.getDeviceLabel() != null ? session.getDeviceLabel() : "Bilinmeyen cihaz",
                session.getIpAddress(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getSessionId().equals(currentSessionId)
        );
    }
}
