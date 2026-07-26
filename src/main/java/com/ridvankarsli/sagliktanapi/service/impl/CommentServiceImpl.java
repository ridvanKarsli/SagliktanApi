package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Comment create(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Gönderi bulunamadı"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .build();

        return commentRepository.save(comment);
    }

    @Override
    public Page<Comment> listByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable);
    }

    @Override
    @Transactional
    public Comment update(Long commentId, Long requesterId, boolean requesterIsAdmin, String content) {
        Comment comment = getById(commentId);
        assertOwnerOrAdmin(comment.getUser().getId(), requesterId, requesterIsAdmin);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long requesterId, boolean requesterIsAdmin) {
        Comment comment = getById(commentId);
        assertOwnerOrAdmin(comment.getUser().getId(), requesterId, requesterIsAdmin);
        commentRepository.delete(comment);
    }

    private Comment getById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı"));
    }

    private void assertOwnerOrAdmin(Long ownerId, Long requesterId, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        if (!ownerId.equals(requesterId)) {
            throw new ForbiddenException("Bu işlem için yetkiniz yok");
        }
    }
}
