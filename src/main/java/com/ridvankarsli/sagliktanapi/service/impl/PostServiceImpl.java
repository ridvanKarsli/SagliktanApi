package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.SubGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final SubGroupRepository subGroupRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Post create(Long subGroupId, Long userId, String title, String content) {
        SubGroup subGroup = subGroupRepository.findById(subGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Alt grup bulunamadı"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        Post post = Post.builder()
                .subGroup(subGroup)
                .user(user)
                .title(title)
                .content(content)
                .build();

        return postRepository.save(post);
    }

    @Override
    public Post getById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gönderi bulunamadı"));
    }

    @Override
    public Page<Post> listBySubGroup(Long subGroupId, Pageable pageable) {
        return postRepository.findBySubGroupId(subGroupId, pageable);
    }

    @Override
    public Page<Post> listByUser(Long userId, Pageable pageable) {
        return postRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<Post> search(String query, Pageable pageable) {
        return postRepository.search(query, pageable);
    }

    @Override
    @Transactional
    public Post update(Long postId, Long requesterId, boolean requesterIsAdmin, String title, String content) {
        Post post = getById(postId);
        assertOwnerOrAdmin(post.getUser().getId(), requesterId, requesterIsAdmin);

        post.setTitle(title);
        post.setContent(content);
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public void delete(Long postId, Long requesterId, boolean requesterIsAdmin) {
        Post post = getById(postId);
        assertOwnerOrAdmin(post.getUser().getId(), requesterId, requesterIsAdmin);
        postRepository.delete(post);
    }

    // Rapor 5.3: "kendi" kaydı üzerindeki işlemler için sahiplik kontrolü
    // service katmanında yapılmalı, sadece role bazlı kontrol yeterli değil.
    private void assertOwnerOrAdmin(Long ownerId, Long requesterId, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        if (!ownerId.equals(requesterId)) {
            throw new ForbiddenException("Bu işlem için yetkiniz yok");
        }
    }
}
