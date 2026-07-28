package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    // Post detay sayfasında sadece üst-seviye yorumlar sayfalanır, tüm alt
    // yanıtlar (her derinlikten) tek sorguyla ayrıca getirilip bellekte
    // ağaca dönüştürülür - bkz. CommentResponse.buildTree.
    // OrderByCreatedAtAsc: alt yanıtlarla aynı kronolojik sırada (en eski
    // üstte) - ayrıca ORDER BY olmadan sayfalar arası sıra garanti edilmez.
    Page<Comment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtAsc(Long postId, Pageable pageable);

    List<Comment> findByPostIdAndParentCommentIdIsNotNullOrderByCreatedAtAsc(Long postId);

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
