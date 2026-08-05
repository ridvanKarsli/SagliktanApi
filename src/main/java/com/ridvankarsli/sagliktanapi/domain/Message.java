package com.ridvankarsli.sagliktanapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// Faz 2 adım 6: bir konuşma içindeki tek mesaj. attachmentKey, adım 4'ün
// fotoğraf altyapısını (MediaStorageService/R2) yeniden kullanır - mesaj
// başına tek fotoğraf yeterli olduğu için PostAttachment'taki gibi ayrı bir
// tablo/sortOrder'a gerek yok, doğrudan bu satırda tutuluyor.
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"conversation", "sender"})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    private String content;

    @Column(name = "attachment_key", length = 512)
    private String attachmentKey;

    // Faz 2 adım 7: gönderiyi mesaj olarak paylaşma. Post silinirse
    // ON DELETE SET NULL (bkz. V16 migration) - mesaj satırı kalır, sadece
    // referansı düşer; ChatMessageResponse bunu "gönderi silinmiş" olarak
    // yorumlar.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_post_id")
    private Post sharedPost;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
