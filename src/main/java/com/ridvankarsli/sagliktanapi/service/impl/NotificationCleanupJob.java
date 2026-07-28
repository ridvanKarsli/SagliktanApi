package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

// Okunmuş bildirimler kullanıcı için artık bir değer taşımıyor - bu job
// bunları oluşturulmalarından 1 gün sonra veritabanından temizler.
// Okunmamış bildirimlere hiç dokunulmaz (kullanıcı görene kadar süresiz kalır).
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private static final long RETENTION_DAYS = 1;

    private final NotificationRepository notificationRepository;

    // Sabit gecikmeli (fixedDelay): bir önceki çalışma bitmeden yenisi
    // başlamaz - uzun süren bir silme işlemi üst üste binmez. Her 1 saatte
    // bir çalışması, 1 günlük saklama süresi için yeterince sık.
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void deleteOldReadNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = notificationRepository.deleteByReadTrueAndCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Bildirim temizliği: {} okunmuş bildirim silindi (cutoff={})", deleted, cutoff);
        }
    }
}
