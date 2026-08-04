package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    Post create(Long subGroupId, Long userId, String title, String content);

    Post getById(Long id);

    Page<Post> listBySubGroup(Long subGroupId, PostSortOption sort, Pageable pageable);

    Page<Post> listByUser(Long userId, Pageable pageable);

    Page<Post> search(String query, Pageable pageable);

    Post update(Long postId, Long requesterId, boolean requesterIsAdmin, String title, String content);

    void delete(Long postId, Long requesterId, boolean requesterIsAdmin);
}
