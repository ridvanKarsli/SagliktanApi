package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // OrderByCreatedAtDesc: Pageable sort taşımıyorsa (client sort
    // göndermiyorsa) sayfalar arası deterministik sıra garantisi yok -
    // ORDER BY olmadan Postgres satır sırası garanti edilmez, bu da art
    // arda gelen sayfalarda aynı postun tekrar görünmesine ya da bir
    // postun hiç görünmemesine yol açabilir.
    Page<Post> findBySubGroupIdOrderByCreatedAtDesc(Long subGroupId, Pageable pageable);

    // Faz 2 adım 1: popülerlik sıralaması (?sort=popular). Reaction hedefe
    // (post/comment) polimorfik (target_type + target_id, FK yok - bkz.
    // Reaction entity yorumu) bağlandığı için JPQL join kurulamıyor, search()
    // metodundaki gibi native query gerekiyor. LEFT JOIN sayesinde hiç
    // reaksiyonu olmayan gönderiler de (0 sayıyla) listede kalıyor; eşit
    // sayıda created_at DESC ikincil kriter olarak devreye giriyor -
    // deterministik sayfalama garantisi yukarıdaki OrderByCreatedAtDesc
    // metoduyla aynı gerekçeyle (ORDER BY olmadan Postgres satır sırası
    // garanti değil).
    // DİKKAT: bu native sorgu kendi ORDER BY'ını içeriyor - çağıran taraf
    // (PostServiceImpl) Pageable'ı SearchQueryUtil.stripSort ile vermeli,
    // aksi halde search()'te daha önce yaşanan "çift ORDER BY" Postgres
    // syntax hatası (bkz. SearchQueryUtil javadoc) burada da tekrarlanır.
    @Query(
            value = "SELECT p.* FROM posts p " +
                    "LEFT JOIN reactions r ON r.target_type = 'POST' AND r.target_id = p.id AND r.value = 'HELPFUL' " +
                    "WHERE p.sub_group_id = :subGroupId " +
                    "GROUP BY p.id " +
                    "ORDER BY COUNT(r.id) DESC, p.created_at DESC",
            countQuery = "SELECT count(*) FROM posts p WHERE p.sub_group_id = :subGroupId",
            nativeQuery = true)
    Page<Post> findBySubGroupIdOrderByReactionCountDesc(@Param("subGroupId") Long subGroupId, Pageable pageable);

    // Alt grup listesinde gösterilen sohbet (post) sayısı
    long countBySubGroupId(Long subGroupId);

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Profil sayfasındaki "Gönderi" istatistiği - bkz. UserController.
    long countByUserId(Long userId);

    // Rapor 4.5 + arama iyileştirmesi (V11/V12): prefix tsquery string'i
    // artık Java'da (bkz. SearchQueryUtil) inşa edilip hazır olarak
    // veriliyor - Postgres tarafında sadece standart to_tsquery/word_similarity
    // fonksiyonları çağrılıyor, özel bir PL/SQL fonksiyonuna bağımlılık yok.
    // Prefix eşleşme (:tsQuery) VE pg_trgm word_similarity ile yazım hatası
    // toleranslı eşleşme (:rawQuery) aynı sorguda OR ile birleşiyor,
    // GREATEST(...) skoruna göre en alakalı sonuç en üstte.
    @Query(
            value = "SELECT * FROM posts p WHERE " +
                    "p.search_vector @@ to_tsquery('turkish', :tsQuery) " +
                    "OR word_similarity(:rawQuery, p.title || ' ' || p.content) > 0.3 " +
                    "ORDER BY GREATEST(" +
                    "    COALESCE(ts_rank(p.search_vector, to_tsquery('turkish', :tsQuery)), 0), " +
                    "    word_similarity(:rawQuery, p.title || ' ' || p.content)" +
                    ") DESC",
            countQuery = "SELECT count(*) FROM posts p WHERE " +
                    "p.search_vector @@ to_tsquery('turkish', :tsQuery) " +
                    "OR word_similarity(:rawQuery, p.title || ' ' || p.content) > 0.3",
            nativeQuery = true)
    Page<Post> search(@Param("rawQuery") String rawQuery, @Param("tsQuery") String tsQuery, Pageable pageable);
}
