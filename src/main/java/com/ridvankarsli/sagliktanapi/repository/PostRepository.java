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

    // Rapor 4.5: PostgreSQL Full-Text Search (search_vector, migration'daki
    // GENERATED ALWAYS AS (...) STORED kolonu ve GIN index üzerinden)
    @Query(
            value = "SELECT * FROM posts p WHERE p.search_vector @@ plainto_tsquery('turkish', :query)",
            countQuery = "SELECT count(*) FROM posts p WHERE p.search_vector @@ plainto_tsquery('turkish', :query)",
            nativeQuery = true)
    Page<Post> search(@Param("query") String query, Pageable pageable);
}
