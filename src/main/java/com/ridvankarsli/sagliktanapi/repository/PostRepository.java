package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findBySubGroupId(Long subGroupId, Pageable pageable);

    // Alt grup listesinde gösterilen sohbet (post) sayısı
    long countBySubGroupId(Long subGroupId);

    Page<Post> findByUserId(Long userId, Pageable pageable);

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
