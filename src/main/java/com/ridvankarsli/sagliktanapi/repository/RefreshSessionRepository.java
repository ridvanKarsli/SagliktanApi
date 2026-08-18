package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {

    Optional<RefreshSession> findBySessionId(String sessionId);

    List<RefreshSession> findByUserIdAndRevokedFalseOrderByLastUsedAtDesc(Long userId);

    // Hesap silme/anonimleştirme akışında (bkz. UserServiceImpl.deleteAccount)
    // kullanıcının tüm oturum kayıtlarını temizlemek için.
    void deleteByUserId(Long userId);

    // Süresi geçeli uzun olmuş satırları düzenli temizlemek için (bkz.
    // RefreshSessionCleanupJob) - ne revoked ne de expired olanlar hiç
    // silinmiyor, tabloyu makul boyutta tutmak amaçlı bakım. Dönüş değeri
    // (silinen satır sayısı) NotificationCleanupJob ile aynı konvansiyon.
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
