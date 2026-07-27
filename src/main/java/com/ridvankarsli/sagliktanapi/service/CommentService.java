package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {

    // parentCommentId null ise üst-seviye yorum, dolu ise bir yoruma yanıt
    // oluşturulur. Yanıta yanıt verilirse otomatik olarak en üstteki yoruma
    // bağlanır (tek seviye nested reply) - bkz. CommentServiceImpl.resolveParent.
    Comment create(Long postId, Long userId, String content, Long parentCommentId);

    // Sadece üst-seviye yorumları sayfalar; her yorumun yanıtları ayrı
    // getirilir (bkz. CommentServiceImpl.listReplies).
    Page<Comment> listByPost(Long postId, Pageable pageable);

    List<Comment> listReplies(Long commentId);

    Comment update(Long commentId, Long requesterId, boolean requesterIsAdmin, String content);

    void delete(Long commentId, Long requesterId, boolean requesterIsAdmin);
}
