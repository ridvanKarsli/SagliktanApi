package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.MessageRequest;
import com.ridvankarsli.sagliktanapi.domain.MessageRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRequestRepository extends JpaRepository<MessageRequest, Long> {

    Optional<MessageRequest> findBySenderIdAndRecipientIdAndStatus(
            Long senderId, Long recipientId, MessageRequestStatus status);

    Page<MessageRequest> findByRecipientIdAndStatusOrderByCreatedAtDesc(
            Long recipientId, MessageRequestStatus status, Pageable pageable);

    long countByRecipientIdAndStatus(Long recipientId, MessageRequestStatus status);

    // Kullanıcının kendi gönderdiği, hâlâ yanıt bekleyen istekler - "Giden
    // istekler" sekmesi + iptal akışı için.
    Page<MessageRequest> findBySenderIdAndStatusOrderByCreatedAtDesc(
            Long senderId, MessageRequestStatus status, Pageable pageable);
}
