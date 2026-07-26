package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    Comment create(Long postId, Long userId, String content);

    Page<Comment> listByPost(Long postId, Pageable pageable);

    Comment update(Long commentId, Long requesterId, boolean requesterIsAdmin, String content);

    void delete(Long commentId, Long requesterId, boolean requesterIsAdmin);
}
