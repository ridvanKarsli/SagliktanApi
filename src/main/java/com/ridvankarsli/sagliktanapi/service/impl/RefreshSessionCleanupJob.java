package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.repository.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

// Süresi dolmuş refresh_sessions satırlarını düzenli temizler (bkz. V19
// migration). Revoked ama henüz süresi dolmamış satırlara DOKUNULMAZ -
// kullanıcının "Aktif Oturumlar" listesinde bir oturumu manuel sonlandırdığı
// an değil, o oturumun doğal ömrü (refresh token TTL'i) dolduğunda silinir.
// NotificationCleanupJob ile aynı desen (bkz. o dosya).
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshSessionCleanupJob {

    private final RefreshSessionRepository refreshSessionRepository;

    // 6 saatte bir yeterli - satırlar zaten expires_at'e göre silindiği için
    // sık çalışmanın bir faydası yok, sadece gereksiz DB yükü olurdu.
    @Scheduled(fixedDelay = 6, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void deleteExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now();
        long deleted = refreshSessionRepository.deleteByExpiresAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Oturum temizliği: {} süresi dolmuş refresh_session silindi (cutoff={})", deleted, cutoff);
        }
    }
}
