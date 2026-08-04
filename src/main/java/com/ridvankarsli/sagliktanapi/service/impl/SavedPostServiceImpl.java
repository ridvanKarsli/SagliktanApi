package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SavedPost;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.SavedPostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.SavedPostService;
import com.ridvankarsli.sagliktanapi.util.SearchQueryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SavedPostServiceImpl implements SavedPostService {

    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void save(Long userId, Long postId) {
        // Zaten kaydedilmişse sessizce çık - tekrar kaydetme isteği (ör. çift
        // tıklama) hata değil, idempotent bir no-op olmalı.
        if (savedPostRepository.existsByUserIdAndPostId(userId, postId)) {
            return;
        }
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Gönderi bulunamadı");
        }

        SavedPost savedPost = SavedPost.builder()
                // getReferenceById: principal/path'ten gelen ID'lerin
                // geçerliliği zaten doğrulandı (auth + existsById kontrolü),
                // gereksiz ikinci bir SELECT'e gerek yok - bkz.
                // ReactionServiceImpl.setReaction ile aynı gerekçe.
                .user(userRepository.getReferenceById(userId))
                .post(postRepository.getReferenceById(postId))
                .build();
        savedPostRepository.save(savedPost);
    }

    @Override
    @Transactional
    public void unsave(Long userId, Long postId) {
        savedPostRepository.deleteByUserIdAndPostId(userId, postId);
    }

    @Override
    public boolean isSaved(Long userId, Long postId) {
        if (userId == null) {
            return false;
        }
        return savedPostRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    public Set<Long> findSavedPostIds(Long userId, Collection<Long> postIds) {
        if (userId == null || postIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(savedPostRepository.findSavedPostIds(userId, postIds));
    }

    @Override
    public Page<Post> listSavedByUser(Long userId, Pageable pageable) {
        // findSavedPostsByUserId kendi ORDER BY sp.createdAt desc'ini
        // taşıyor - Faz 2 adım 1'de canlıda yaşanan "Pageable'ın otomatik
        // sort binding'i özel sorgunun ORDER BY'ıyla çakışıyor" hatasını
        // (bkz. PostServiceImpl.listBySubGroup yorumu) burada da tekrar
        // yaşamamak için gelen Pageable'ın Sort'u temizleniyor.
        return savedPostRepository.findSavedPostsByUserId(userId, SearchQueryUtil.stripSort(pageable));
    }
}
