package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.PostAttachment;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// Faz 2 adım 4: bir gönderiye hangi fotoğrafların (R2 storage key'lerinin)
// bağlı olduğunu yönetir. MediaStorageService'in aksine "post" kavramını
// bilir - iş kuralları (adet/tip/boyut sınırı, sahiplik) burada, ham
// depolama erişimi orada (bkz. MediaStorageService javadoc).
public interface PostAttachmentService {

    // storageKeys, MediaStorageService.createPresignedUpload ile üretilmiş
    // ve client tarafından fiilen R2'ye yüklenmiş key'ler olmalı - her biri
    // headObject ile doğrulanır (var mı, izin verilen tip/boyutta mı).
    // Geçersiz bir key varsa BadRequestException fırlatılır ve HİÇBİR
    // attachment kaydedilmez (çağıran @Transactional içindeyse post
    // oluşturma da geri alınır - bkz. PostServiceImpl.create).
    List<PostAttachment> attach(Post post, List<String> storageKeys);

    List<PostAttachment> findByPostId(Long postId);

    // Feed/liste ekranlarında her post için ayrı sorgu atmamak için toplu
    // çekim - Reaction/SavedPost servislerindeki toplu sorgu deseniyle
    // aynı gerekçe. Attachment'ı olmayan post'lar map'te yer almaz,
    // çağıran taraf getOrDefault(id, List.of()) kullanmalı.
    Map<Long, List<PostAttachment>> findByPostIds(Collection<Long> postIds);

    // Post silinirken hem R2'deki nesneleri hem de bu tablodaki satırları
    // temizler (bkz. PostServiceImpl.delete).
    void deleteAllForPost(Long postId);
}
