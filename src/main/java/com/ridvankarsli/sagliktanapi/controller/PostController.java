package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.ReactionTargetType;
import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.dto.request.PostRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ReactionRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ReportRequest;
import com.ridvankarsli.sagliktanapi.dto.response.MessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PostResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.ContentReportService;
import com.ridvankarsli.sagliktanapi.service.PostService;
import com.ridvankarsli.sagliktanapi.service.PostSortOption;
import com.ridvankarsli.sagliktanapi.service.ReactionService;
import com.ridvankarsli.sagliktanapi.service.ReactionSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ContentReportService contentReportService;
    private final ReactionService reactionService;

    @PostMapping("/api/sub-groups/{subGroupId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @PathVariable Long subGroupId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PostRequest request
    ) {
        return PostResponse.from(
                postService.create(subGroupId, principal.getId(), request.title(), request.content())
        );
    }

    // Faz 2 adım 1: ?sort=recent (varsayılan) | popular. Pageable'ın kendi
    // sort binding'i yerine ayrı bir @RequestParam kullanılıyor çünkü
    // "popular" derived/native query'lerde zaten sabit bir ORDER BY
    // taşıyor (bkz. PostServiceImpl) - Pageable.sort buraya karışırsa
    // anlamsız/çakışan bir davranış olurdu.
    @GetMapping("/api/sub-groups/{subGroupId}/posts")
    public PageResponse<PostResponse> listBySubGroup(
            @PathVariable Long subGroupId,
            @RequestParam(defaultValue = "recent") String sort,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Page<Post> page = postService.listBySubGroup(subGroupId, PostSortOption.fromParam(sort), pageable);
        Map<Long, ReactionSummary> reactions = reactionSummaries(page.getContent(), principal);
        return PageResponse.from(page.map(post -> PostResponse.from(post, reactions.get(post.getId()))));
    }

    // Rapor 4.5: PostgreSQL Full-Text Search (platform geneli)
    @GetMapping("/api/posts/search")
    public PageResponse<PostResponse> search(
            @RequestParam String q, Pageable pageable, @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Page<Post> page = postService.search(q, pageable);
        Map<Long, ReactionSummary> reactions = reactionSummaries(page.getContent(), principal);
        return PageResponse.from(page.map(post -> PostResponse.from(post, reactions.get(post.getId()))));
    }

    // Faz 2 adım 2: "Gönderiler" sayfasındaki alt gruba özel arama kutusu.
    // Yukarıdaki genel /api/posts/search platform genelinde arıyor; bu uç
    // sadece {subGroupId} içindeki gönderilerle sınırlı - bkz.
    // PostRepository.searchBySubGroup yorumu (neden ayrı bir metot/uç).
    @GetMapping("/api/sub-groups/{subGroupId}/posts/search")
    public PageResponse<PostResponse> searchBySubGroup(
            @PathVariable Long subGroupId,
            @RequestParam String q,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Page<Post> page = postService.searchBySubGroup(subGroupId, q, pageable);
        Map<Long, ReactionSummary> reactions = reactionSummaries(page.getContent(), principal);
        return PageResponse.from(page.map(post -> PostResponse.from(post, reactions.get(post.getId()))));
    }

    @GetMapping("/api/posts/{id}")
    public PostResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Post post = postService.getById(id);
        return PostResponse.from(post, reactionService.getSummary(ReactionTargetType.POST, id, principal.getId()));
    }

    @PutMapping("/api/posts/{id}")
    public PostResponse update(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PostRequest request
    ) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        Post post = postService.update(id, principal.getId(), isAdmin, request.title(), request.content());
        return PostResponse.from(post, reactionService.getSummary(ReactionTargetType.POST, id, principal.getId()));
    }

    @DeleteMapping("/api/posts/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        postService.delete(id, principal.getId(), isAdmin);
    }

    @PostMapping("/api/posts/{id}/report")
    public MessageResponse report(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody(required = false) ReportRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        contentReportService.report(ReportTargetType.POST, id, principal.getId(), reason);
        return new MessageResponse("Şikayetiniz alındı, teşekkür ederiz");
    }

    // Faydalı / Faydalı Değil reaksiyonu ver ya da (zaten varsa) değiştir - upsert.
    @PutMapping("/api/posts/{id}/reactions")
    public ReactionSummary react(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReactionRequest request
    ) {
        reactionService.setReaction(ReactionTargetType.POST, id, principal.getId(), request.value());
        return reactionService.getSummary(ReactionTargetType.POST, id, principal.getId());
    }

    @DeleteMapping("/api/posts/{id}/reactions")
    public ReactionSummary removeReaction(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        reactionService.removeReaction(ReactionTargetType.POST, id, principal.getId());
        return reactionService.getSummary(ReactionTargetType.POST, id, principal.getId());
    }

    private Map<Long, ReactionSummary> reactionSummaries(List<Post> posts, CustomUserDetails principal) {
        List<Long> ids = posts.stream().map(Post::getId).toList();
        return reactionService.getSummaries(ReactionTargetType.POST, ids, principal.getId());
    }
}
