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
import com.ridvankarsli.sagliktanapi.service.SavedPostService;
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
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ContentReportService contentReportService;
    private final ReactionService reactionService;
    private final SavedPostService savedPostService;

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
        return toPageResponse(page, principal);
    }

    // Rapor 4.5: PostgreSQL Full-Text Search (platform geneli)
    @GetMapping("/api/posts/search")
    public PageResponse<PostResponse> search(
            @RequestParam String q, Pageable pageable, @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Page<Post> page = postService.search(q, pageable);
        return toPageResponse(page, principal);
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
        return toPageResponse(page, principal);
    }

    @GetMapping("/api/posts/{id}")
    public PostResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Post post = postService.getById(id);
        return toPostResponse(post, principal);
    }

    @PutMapping("/api/posts/{id}")
    public PostResponse update(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PostRequest request
    ) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        Post post = postService.update(id, principal.getId(), isAdmin, request.title(), request.content());
        return toPostResponse(post, principal);
    }

    // Faz 2 adım 3: gönderiyi kaydet/kaydı kaldır (yıldızlama). Reaksiyon
    // uçlarıyla aynı REST desenini izliyor: PUT = kaydet (idempotent - zaten
    // kaydedilmişse no-op), DELETE = kaydı kaldır. Gövde döndürmeye gerek
    // yok, frontend zaten optimistic UI güncelliyor.
    @PutMapping("/api/posts/{id}/saved")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePost(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        savedPostService.save(principal.getId(), id);
    }

    @DeleteMapping("/api/posts/{id}/saved")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsavePost(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        savedPostService.unsave(principal.getId(), id);
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

    // Sayfalanmış bir Post listesini, reaksiyon + kaydetme durumunu/sayısını
    // toplu (N+1 sorgu değil) çekip PostResponse'a çeviren ortak yol -
    // listBySubGroup/search/searchBySubGroup arasında tekrar etmesin diye
    // tek yerde.
    private PageResponse<PostResponse> toPageResponse(Page<Post> page, CustomUserDetails principal) {
        List<Post> posts = page.getContent();
        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<Long, ReactionSummary> reactions = reactionService.getSummaries(ReactionTargetType.POST, ids, principal.getId());
        Set<Long> savedIds = savedPostService.findSavedPostIds(principal.getId(), ids);
        Map<Long, Long> savedCounts = savedPostService.countByPostIds(ids);
        return PageResponse.from(page.map(post ->
                PostResponse.from(post, reactions.get(post.getId()), savedIds.contains(post.getId()),
                        savedCounts.get(post.getId()))));
    }

    // Tek bir Post için (getById/update) aynı zenginleştirme.
    private PostResponse toPostResponse(Post post, CustomUserDetails principal) {
        ReactionSummary reactions = reactionService.getSummary(ReactionTargetType.POST, post.getId(), principal.getId());
        boolean saved = savedPostService.isSaved(principal.getId(), post.getId());
        long savedCount = savedPostService.countByPostIds(List.of(post.getId())).getOrDefault(post.getId(), 0L);
        return PostResponse.from(post, reactions, saved, savedCount);
    }
}
