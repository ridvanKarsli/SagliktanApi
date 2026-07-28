package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient.id = :recipientId and n.read = false")
    void markAllReadForRecipient(@Param("recipientId") Long recipientId);

    // Okunmuş bildirimler kalıcı geçmiş için değil, sadece "kaçırdıysan
    // görebilesin" amaçlı - kullanıcı okuduktan sonra saklanmasının bir
    // değeri yok. NotificationCleanupJob tarafından periyodik çağrılır.
    // NOT: readAt yok (schema'da tutulmuyor) - "okunma" anını değil,
    // bildirimin oluşturulma anını referans alıyoruz.
    @Modifying
    @Query("delete from Notification n where n.read = true and n.createdAt < :cutoff")
    int deleteByReadTrueAndCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
