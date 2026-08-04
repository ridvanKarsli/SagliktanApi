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

    // Faz 2 adım 1 (+ adım 3b'de genişletildi): popülerlik sıralaması
    // (?sort=popular). Hem reaksiyon hem kaydedilme sayısı popülerliğe dahil
    // - ikisi de post'a polimorfik/ayrı tablo üzerinden bağlandığı için
    // JPQL join kurulamıyor, search() metodundaki gibi native query
    // gerekiyor. Her iki child tablo da aynı sorguda LEFT JOIN edildiğinde
    // satırlar çarpımsal (fan-out) çoğaldığından COUNT(r.id) yerine
    // COUNT(DISTINCT r.id) kullanılmalı - aksi halde bir post için hem
    // reaksiyon hem kaydedilme sayısı gerçek değerin üzerinde hesaplanır.
    // LEFT JOIN sayesinde hiç reaksiyonu/kaydı olmayan gönderiler de (0
    // sayıyla) listede kalıyor; eşit toplamda created_at DESC ikincil
    // kriter olarak devreye giriyor - deterministik sayfalama garantisi
    // yukarıdaki OrderByCreatedAtDesc metoduyla aynı gerekçeyle (ORDER BY
    // olmadan Postgres satır sırası garanti değil).
    // DİKKAT: bu native sorgu kendi ORDER BY'ını içeriyor - çağıran taraf
    // (PostServiceImpl) Pageable'ı SearchQueryUtil.stripSort ile vermeli,
    // aksi halde search()'te daha önce yaşanan "çift ORDER BY" Postgres
    // syntax hatası (bkz. SearchQueryUtil javadoc) burada da tekrarlanır.
    @Query(
            value = "SELECT p.* FROM posts p " +
                    "LEFT JOIN reactions r ON r.target_type = 'POST' AND r.target_id = p.id AND r.value = 'HELPFUL' " +
                    "LEFT JOIN saved_posts sp ON sp.post_id = p.id " +
                    "WHERE p.sub_group_id = :subGroupId " +
                    "GROUP BY p.id " +
                    "ORDER BY (COUNT(DISTINCT r.id) + COUNT(DISTINCT sp.id)) DESC, p.created_at DESC",
            countQuery = "SELECT count(*) FROM posts p WHERE p.sub_group_id = :subGroupId",
            nativeQuery = true)
    Page<Post> findBySubGroupIdOrderByPopularityDesc(@Param("subGroupId") Long subGroupId, Pageable pageable);

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
    //
    // SEARCH_MATCH_CONDITION/SEARCH_RELEVANCE_ORDER: search() ile Faz 2
    // adım 2'de eklenen searchBySubGroup() aynı eşleşme/sıralama mantığını
    // paylaşıyor (tek fark: ikincisi p.sub_group_id ile filtreliyor). Bu
    // fragmanı iki sorguda ayrı ayrı yazmak yerine tek yerde tanımlayıp
    // paylaşmak, biri güncellenince diğerinin unutulmasını (drift) önlüyor.
    // @Query value'su compile-time sabit olmak zorunda olduğu için bu
    // alanlar static final String olarak tanımlı.
    String SEARCH_MATCH_CONDITION =
            "(p.search_vector @@ to_tsquery('turkish', :tsQuery) " +
                    "OR word_similarity(:rawQuery, p.title || ' ' || p.content) > 0.3)";
    String SEARCH_RELEVANCE_ORDER =
            "GREATEST(" +
                    "    COALESCE(ts_rank(p.search_vector, to_tsquery('turkish', :tsQuery)), 0), " +
                    "    word_similarity(:rawQuery, p.title || ' ' || p.content)" +
                    ") DESC";

    @Query(
            value = "SELECT * FROM posts p WHERE " + SEARCH_MATCH_CONDITION + " ORDER BY " + SEARCH_RELEVANCE_ORDER,
            countQuery = "SELECT count(*) FROM posts p WHERE " + SEARCH_MATCH_CONDITION,
            nativeQuery = true)
    Page<Post> search(@Param("rawQuery") String rawQuery, @Param("tsQuery") String tsQuery, Pageable pageable);

    // Faz 2 adım 2: "Gönderiler" sayfasındaki alt gruba özel arama kutusu.
    // search()'ün platform genelindeki halinin aksine sadece tek bir alt
    // gruptaki gönderilerle sınırlı - ayrı bir endpoint/metot olarak
    // tutuluyor (search()'e opsiyonel bir subGroupId parametresi eklemek
    // yerine) çünkü platform geneli arama (bkz. Search.jsx) ve gruba özel
    // arama iki farklı, birbirinden bağımsız evrilebilecek kullanım
    // senaryosu - tek metodu boolean/nullable parametreyle dallandırmak
    // yerine ayrı, tek sorumluluğu net iki metot tercih edildi.
    @Query(
            value = "SELECT * FROM posts p WHERE p.sub_group_id = :subGroupId AND " + SEARCH_MATCH_CONDITION
                    + " ORDER BY " + SEARCH_RELEVANCE_ORDER,
            countQuery = "SELECT count(*) FROM posts p WHERE p.sub_group_id = :subGroupId AND " + SEARCH_MATCH_CONDITION,
            nativeQuery = true)
    Page<Post> searchBySubGroup(
            @Param("subGroupId") Long subGroupId,
            @Param("rawQuery") String rawQuery,
            @Param("tsQuery") String tsQuery,
            Pageable pageable);
}
