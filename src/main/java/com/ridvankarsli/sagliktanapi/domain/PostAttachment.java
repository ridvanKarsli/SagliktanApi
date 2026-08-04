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

// Faz 2 adım 4: bir gönderiye eklenen fotoğraf. Asıl dosya Cloudflare
// R2'de duruyor (bkz. MediaStorageServiceImpl) - burada sadece storage
// key'i ve galeri içindeki sırası tutuluyor. sortOrder olmadan Postgres'in
// döndürdüğü sıra garanti değil (bkz. diğer sıralı sorgu yorumları),
// kullanıcının seçtiği fotoğraf sırası bozulabilirdi.
@Entity
@Table(name = "post_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"post"})
public class PostAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
