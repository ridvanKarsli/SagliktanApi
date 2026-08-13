package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    // Post detay sayfasında sadece üst-seviye yorumlar sayfalanır.
    // OrderByCreatedAtAsc: alt yanıtlarla aynı kronolojik sırada (en eski
    // üstte) - ayrıca ORDER BY olmadan sayfalar arası sıra garanti edilmez.
    Page<Comment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtAsc(Long postId, Pageable pageable);

    // Bir yorumun DOĞRUDAN yanıtlarını sayfalı döner (thread-drill: bkz.
    // CommentService.listReplies). Eskiden tüm postun tüm alt yanıtları
    // (her derinlikten) sınırsız tek sorguda çekilip belleğe ağaç olarak
    // kuruluyordu - binlerce yanıtlı bir postta bu, tek istekte tüm alt
    // ağacı belleğe/ağa çekmek anlamına geliyordu. Artık her seviye,
    // kullanıcı o dalı gerçekten açtığında ayrı ayrı ve sayfalı getiriliyor.
    Page<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId, Pageable pageable);

    // Birden çok yorumun DOĞRUDAN yanıt sayısını tek sorguda, toplu döner
    // (bkz. ReactionRepository.countGrouped ile aynı desen) - yorum listesi
    // ekranında her yorum için ayrı count sorgusu atmamak için. Silinmiş
    // yanıtlar da sayılır (soft-delete sonrası da bir "yer" kaplamaya, yani
    // yer tutucu metinle görünmeye devam ediyorlar).
    @Query("select c.parentComment.id as parentId, count(c) as count "
            + "from Comment c where c.parentComment.id in :parentIds "
            + "group by c.parentComment.id")
    List<ReplyCountRow> countRepliesGrouped(@Param("parentIds") Collection<Long> parentIds);

    interface ReplyCountRow {
        Long getParentId();
        long getCount();
    }

    // Profil sayfasındaki "Yorum" istatistiği - silinmiş yorumlar sayılmaz.
    long countByUserIdAndDeletedFalse(Long userId);

    // KVKK veri dışa aktarma (bkz. UserServiceImpl.exportData) - kullanıcının
    // TÜM yorumlarını (silinmiş olanlar dahil, kendi verisi olduğu için)
    // kronolojik sırayla döner.
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Gelişmiş arama (V11/V12): prefix tsquery string'i Java'da inşa edilip
    // (bkz. SearchQueryUtil) hazır veriliyor - bkz. PostRepository.search'teki
    // aynı desen açıklaması. Silinmiş (soft delete) yorumlar sonuçlara girmez.
    @Query(
            value = "SELECT * FROM comments c WHERE c.deleted = false AND (" +
                    "    c.search_vector @@ to_tsquery('turkish', :tsQuery) " +
                    "    OR word_similarity(:rawQuery, c.content) > 0.3" +
                    ") ORDER BY GREATEST(" +
                    "    COALESCE(ts_rank(c.search_vector, to_tsquery('turkish', :tsQuery)), 0), " +
                    "    word_similarity(:rawQuery, c.content)" +
                    ") DESC",
            countQuery = "SELECT count(*) FROM comments c WHERE c.deleted = false AND (" +
                    "    c.search_vector @@ to_tsquery('turkish', :tsQuery) " +
                    "    OR word_similarity(:rawQuery, c.content) > 0.3" +
                    ")",
            nativeQuery = true)
    Page<Comment> search(@Param("rawQuery") String rawQuery, @Param("tsQuery") String tsQuery, Pageable pageable);
}
