package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    // Post detay sayfasında sadece üst-seviye yorumlar sayfalanır, yanıtlar
    // her yorumla birlikte ayrıca (sayfalanmadan) getirilir.
    Page<Comment> findByPostIdAndParentCommentIdIsNull(Long postId, Pageable pageable);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);
}
