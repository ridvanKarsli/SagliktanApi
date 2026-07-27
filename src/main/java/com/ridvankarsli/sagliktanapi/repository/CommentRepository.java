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
    Page<Comment> findByPostIdAndParentCommentIdIsNull(Long postId, Pageable pageable);

    List<Comment> findByPostIdAndParentCommentIdIsNotNullOrderByCreatedAtAsc(Long postId);

    // Gelişmiş arama: yorum içeriğinde tam metin arama (bkz. V7 migration,
    // search_vector kolonu). Silinmiş (soft delete) yorumlar sonuçlara
    // girmez.
    @Query(
            value = "SELECT * FROM comments c WHERE c.deleted = false " +
                    "AND c.search_vector @@ plainto_tsquery('turkish', :query)",
            countQuery = "SELECT count(*) FROM comments c WHERE c.deleted = false " +
                    "AND c.search_vector @@ plainto_tsquery('turkish', :query)",
            nativeQuery = true)
    Page<Comment> search(@Param("query") String query, Pageable pageable);
}
