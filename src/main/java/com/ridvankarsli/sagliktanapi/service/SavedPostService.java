package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

public interface SavedPostService {

    void save(Long userId, Long postId);

    void unsave(Long userId, Long postId);

    boolean isSaved(Long userId, Long postId);

    // Feed/liste ekranlarında her post için tek tek sorgu atmamak için toplu
    // kontrol - bkz. ReactionService.getSummaries ile aynı gerekçe.
    Set<Long> findSavedPostIds(Long userId, Collection<Long> postIds);

    Page<Post> listSavedByUser(Long userId, Pageable pageable);
}
