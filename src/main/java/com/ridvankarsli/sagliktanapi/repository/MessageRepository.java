package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    // Bir konuşmayı açtığında karşı tarafın (kendi göndermediklerin) tüm
    // okunmamış mesajlarını tek seferde okunmuş işaretlemek için - mesaj
    // mesaj ayrı ayrı PUT atmak yerine.
    @Modifying
    @Query("update Message m set m.readAt = CURRENT_TIMESTAMP "
            + "where m.conversation.id = :conversationId and m.sender.id <> :readerId and m.readAt is null")
    int markConversationRead(@Param("conversationId") Long conversationId, @Param("readerId") Long readerId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long senderId);

    // Sohbet listesinde her konuşma için "son mesaj" önizlemesi - konuşma
    // başına N ayrı sorgu atmamak için tek native sorguda toplu çekiliyor.
    // DISTINCT ON Postgres'e özgü olduğu için JPQL değil native query
    // (PostRepository'deki native sorgu kullanım deseniyle aynı gerekçe).
    @Query(value = "SELECT DISTINCT ON (conversation_id) * FROM messages "
            + "WHERE conversation_id IN (:conversationIds) ORDER BY conversation_id, created_at DESC",
            nativeQuery = true)
    List<Message> findLastMessagesForConversations(@Param("conversationIds") Collection<Long> conversationIds);

    // Sohbet listesinde her konuşma için okunmamış sayısı - yine
    // SavedPostRepository.countGrouped ile aynı toplu-sorgu deseni.
    @Query("select m.conversation.id as conversationId, count(m) as count from Message m "
            + "where m.conversation.id in :conversationIds and m.sender.id <> :readerId and m.readAt is null "
            + "group by m.conversation.id")
    List<UnreadCountRow> countUnreadGrouped(
            @Param("conversationIds") Collection<Long> conversationIds, @Param("readerId") Long readerId);

    interface UnreadCountRow {
        Long getConversationId();
        long getCount();
    }

    // Nav rozeti için: kullanıcının TÜM konuşmalarındaki toplam okunmamış
    // mesaj sayısı - countUnreadGrouped'daki gibi konuşma bazlı değil, tek
    // sayı (bkz. MessagingContext.refreshUnreadCount).
    @Query("select count(m) from Message m "
            + "where (m.conversation.userOne.id = :userId or m.conversation.userTwo.id = :userId) "
            + "and m.sender.id <> :userId and m.readAt is null")
    long countUnreadForUser(@Param("userId") Long userId);
}
