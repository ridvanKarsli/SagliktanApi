package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // userOneId/userTwoId çağıran taraftan HER ZAMAN canonicalize edilmiş
    // (küçük/büyük id) olarak gelmeli - bkz. ConversationServiceImpl.
    Optional<Conversation> findByUserOneIdAndUserTwoId(Long userOneId, Long userTwoId);

    // Sıralama şimdilik konuşmanın açılma anına göre - "son mesaja göre
    // sırala" için messages ile join gereken ayrı bir sorgu, sohbet listesi
    // UI'ı (F2) yazılırken burada eklenecek.
    @Query("select c from Conversation c where c.userOne.id = :userId or c.userTwo.id = :userId order by c.id desc")
    Page<Conversation> findAllForUser(@Param("userId") Long userId, Pageable pageable);
}
