package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.dto.response.CommentResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PostResponse;
import com.ridvankarsli.sagliktanapi.dto.response.SearchResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UserSearchResponse;
import com.ridvankarsli.sagliktanapi.service.CommentService;
import com.ridvankarsli.sagliktanapi.service.PostService;
import com.ridvankarsli.sagliktanapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Twitter/arama çubuğu tarzı birleşik "hızlı arama": tek istekte post,
// yorum ve kişi sonuçlarından en alakalı ilk birkaçını bir arada döner.
// Her kategorinin tam/sayfalı listesi için ayrı uçlar kullanılır:
// /api/posts/search, /api/comments/search, /api/users/search.
@RestController
@RequiredArgsConstructor
public class SearchController {

    private static final int QUICK_SEARCH_LIMIT = 5;

    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;

    @GetMapping("/api/search")
    public SearchResponse search(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return new SearchResponse(List.of(), List.of(), List.of());
        }

        Pageable topResults = PageRequest.of(0, QUICK_SEARCH_LIMIT);

        var posts = postService.search(q, topResults).map(PostResponse::from).getContent();
        var comments = commentService.search(q, topResults).map(CommentResponse::from).getContent();
        var users = userService.search(q, topResults).map(UserSearchResponse::from).getContent();

        return new SearchResponse(posts, comments, users);
    }
}
