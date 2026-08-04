package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    List<PostAttachment> findByPostIdOrderBySortOrderAsc(Long postId);

    // Feed/liste ekranlarında her post için ayrı sorgu atmamak için toplu
    // çekim - ReactionRepository/SavedPostRepository'deki toplu sorgu
    // deseniyle aynı gerekçe. Sonuç PostAttachmentServiceImpl'de post
    // id'sine göre gruplanıyor; postId ASC + sortOrder ASC sıralaması bu
    // gruplamayı ve galeri sırasını tek geçişte doğru kurabilmek için.
    List<PostAttachment> findByPostIdInOrderByPostIdAscSortOrderAsc(@Param("postIds") Collection<Long> postIds);

    // Gönderi silinirken R2'deki nesneleri de silebilmek için önce storage
    // key'leri okunuyor (bkz. PostAttachmentServiceImpl.deleteAllForPost),
    // bu metot o adımdan sonra DB satırlarını temizliyor. post_attachments.
    // post_id FK'i zaten ON DELETE CASCADE ama R2 temizliği için sırayı
    // biz yönetmemiz gerektiğinden bunu açıkça çağırıyoruz.
    void deleteByPostId(Long postId);
}
