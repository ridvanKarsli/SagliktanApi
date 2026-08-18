package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {

    // Faz 2 adım 4: attachmentKeys, MediaStorageService.createPresignedUpload
    // ile üretilip client tarafından R2'ye yüklenmiş storage key'leri -
    // null/boş olabilir (fotoğrafsız gönderi). Düzenleme (update) kapsamına
    // fotoğraf değişikliği DAHİL DEĞİL (bkz. PLAN_faz2_ozellikler.md adım 4) -
    // fotoğraflar sadece oluşturmada eklenir.
    Post create(Long subGroupId, Long userId, String title, String content, List<String> attachmentKeys);

    Post getById(Long id);

    Page<Post> listBySubGroup(Long subGroupId, PostSortOption sort, Pageable pageable);

    Page<Post> listByUser(Long userId, Pageable pageable);

    // Ana sayfa akışı: kullanıcının üye olduğu tüm gruplardaki gönderiler,
    // tek bir zaman sıralı akış hâlinde (bkz. PostRepository.findFeedForUser).
    // Faz6: listBySubGroup ile aynı PostSortOption (RECENT/POPULAR) burada da.
    Page<Post> getFeedForUser(Long userId, PostSortOption sort, Pageable pageable);

    Page<Post> search(String query, Pageable pageable);

    Page<Post> searchBySubGroup(Long subGroupId, String query, Pageable pageable);

    // Admin moderasyonu: sadece fotoğraf içeren gönderiler (bkz.
    // PostRepository.HAS_ATTACHMENTS_CONDITION). tsQuery inşası search() ile
    // birebir aynı olduğu için o mantığı tekrarlamamak adına burada,
    // AdminServiceImpl'in çağırdığı bu iki metotta tutuluyor.
    Page<Post> listWithAttachments(Pageable pageable);

    Page<Post> searchWithAttachments(String query, Pageable pageable);

    Post update(Long postId, Long requesterId, boolean requesterIsAdmin, String title, String content);

    void delete(Long postId, Long requesterId, boolean requesterIsAdmin);

    // Faz6: sabitlenmiş gönderi - X'teki "hakkımda" niteliğindeki bir
    // gönderiyi profilde öne çıkarma özelliği. Sadece gönderi sahibi
    // sabitleyebilir/kaldırabilir (moderasyon amaçlı bir işlem olmadığı
    // için update/delete'in aksine admin override YOK - bkz.
    // PostServiceImpl.pin javadoc'u). Kullanıcı başına en fazla bir
    // sabitlenmiş gönderi (bkz. V20 migration'daki PARTIAL unique index) -
    // yeni bir post sabitlenirken varsa öncekisi otomatik kaldırılır.
    Post pin(Long postId, Long userId);

    Post unpin(Long postId, Long userId);
}
