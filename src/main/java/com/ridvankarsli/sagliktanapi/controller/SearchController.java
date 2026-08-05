package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.ReactionTargetType;
import com.ridvankarsli.sagliktanapi.dto.response.CommentResponse;
import com.ridvankarsli.sagliktanapi.dto.response.SearchResponse;
import com.ridvankarsli.sagliktanapi.dto.response.UserSearchResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.CommentService;
import com.ridvankarsli.sagliktanapi.service.PostResponseAssembler;
import com.ridvankarsli.sagliktanapi.service.PostService;
import com.ridvankarsli.sagliktanapi.service.ReactionService;
import com.ridvankarsli.sagliktanapi.service.ReactionSummary;
import com.ridvankarsli.sagliktanapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    private final ReactionService reactionService;
    private final PostResponseAssembler postResponseAssembler;

    @GetMapping("/api/search")
    public SearchResponse search(@RequestParam String q, @AuthenticationPrincipal CustomUserDetails principal) {
        if (q == null || q.isBlank()) {
            return new SearchResponse(List.of(), List.of(), List.of());
        }

        Pageable topResults = PageRequest.of(0, QUICK_SEARCH_LIMIT);

        List<Post> postResults = postService.search(q, topResults).getContent();
        var posts = postResponseAssembler.assemble(postResults, principal.getId());

        List<Comment> commentResults = commentService.search(q, topResults).getContent();
        Map<Long, ReactionSummary> commentReactions = reactionService.getSummaries(
                ReactionTargetType.COMMENT, commentResults.stream().map(Comment::getId).toList(), principal.getId());
        var comments = commentResults.stream().map(c -> CommentResponse.from(c, commentReactions.get(c.getId()))).toList();

        var users = userService.search(q, topResults).map(UserSearchResponse::from).getContent();

        return new SearchResponse(posts, comments, users);
    }
}
