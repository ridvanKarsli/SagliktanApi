package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.PostAttachment;
import com.ridvankarsli.sagliktanapi.domain.ReactionTargetType;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PostAttachmentResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PostResponse;
import com.ridvankarsli.sagliktanapi.repository.SubGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Bir (veya bir sayfa) Post'u, isteği yapan kullanıcıya göre reaksiyon
// özeti + kaydetme durumu/sayısı + fotoğraflarla zenginleştirip
// PostResponse'a çeviren tek yer. Önceden PostController, UserController ve
// SearchController'da neredeyse birebir aynı ~12 satırlık blok üç kez
// tekrarlanıyordu (bkz. clean-code audit) - artık üçü de buraya delege
// ediyor. Toplu (batch) sorgu deseni korunuyor: reaksiyon/kaydetme/fotoğraf
// N+1 yerine tek sorguyla tüm sayfa için toplu çekiliyor.
@Component
@RequiredArgsConstructor
public class PostResponseAssembler {

    private final ReactionService reactionService;
    private final SavedPostService savedPostService;
    private final PostAttachmentService postAttachmentService;
    private final MediaStorageService mediaStorageService;
    private final SubGroupRepository subGroupRepository;

    public PageResponse<PostResponse> assemble(Page<Post> page, Long viewerId) {
        Enrichment enrichment = enrich(page.getContent(), viewerId);
        return PageResponse.from(page.map(post -> toResponse(post, enrichment)));
    }

    // Ana sayfa akışı: assemble() ile aynı reaksiyon/kaydetme/fotoğraf
    // zenginleştirmesi + ayrıca hangi alt/hastalık grubundan geldiği (bkz.
    // SubGroupRepository.findNameProjectionsByIdIn - N+1'siz tek sorgu).
    // Sadece karışık akışta anlamlı olduğu için ayrı bir metot; assemble()'ı
    // her çağıran yerde (tek alt grup bağlamı zaten belli) gereksiz bir
    // sorguyla yormuyoruz.
    public PageResponse<PostResponse> assembleFeed(Page<Post> page, Long viewerId) {
        List<Post> posts = page.getContent();
        Enrichment enrichment = enrich(posts, viewerId);
        Map<Long, SubGroupRepository.SubGroupNameProjection> namesBySubGroupId = fetchSubGroupNames(posts);
        return PageResponse.from(page.map(post -> {
            SubGroupRepository.SubGroupNameProjection names = namesBySubGroupId.get(post.getSubGroup().getId());
            String subGroupName = names != null ? names.getSubGroupName() : null;
            String diseaseGroupName = names != null ? names.getDiseaseGroupName() : null;
            return PostResponse.from(
                    post,
                    enrichment.reactions().get(post.getId()),
                    enrichment.savedIds().contains(post.getId()),
                    enrichment.savedCounts().get(post.getId()),
                    toAttachmentResponses(enrichment.attachmentsByPost().getOrDefault(post.getId(), List.of())),
                    subGroupName,
                    diseaseGroupName
            );
        }));
    }

    private Map<Long, SubGroupRepository.SubGroupNameProjection> fetchSubGroupNames(List<Post> posts) {
        Set<Long> subGroupIds = posts.stream().map(p -> p.getSubGroup().getId()).collect(Collectors.toSet());
        if (subGroupIds.isEmpty()) {
            return Map.of();
        }
        return subGroupRepository.findNameProjectionsByIdIn(subGroupIds).stream()
                .collect(Collectors.toMap(SubGroupRepository.SubGroupNameProjection::getSubGroupId, p -> p));
    }

    public List<PostResponse> assemble(List<Post> posts, Long viewerId) {
        Enrichment enrichment = enrich(posts, viewerId);
        return posts.stream().map(post -> toResponse(post, enrichment)).toList();
    }

    // Tek bir Post için (ör. getById/update/create sonrası) aynı zenginleştirme.
    public PostResponse assembleOne(Post post, Long viewerId) {
        ReactionSummary reactions = reactionService.getSummary(ReactionTargetType.POST, post.getId(), viewerId);
        boolean saved = savedPostService.isSaved(viewerId, post.getId());
        long savedCount = savedPostService.countByPostIds(List.of(post.getId())).getOrDefault(post.getId(), 0L);
        List<PostAttachmentResponse> attachments = toAttachmentResponses(postAttachmentService.findByPostId(post.getId()));
        return PostResponse.from(post, reactions, saved, savedCount, attachments);
    }

    private Enrichment enrich(List<Post> posts, Long viewerId) {
        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<Long, ReactionSummary> reactions = reactionService.getSummaries(ReactionTargetType.POST, ids, viewerId);
        Set<Long> savedIds = savedPostService.findSavedPostIds(viewerId, ids);
        Map<Long, Long> savedCounts = savedPostService.countByPostIds(ids);
        Map<Long, List<PostAttachment>> attachmentsByPost = postAttachmentService.findByPostIds(ids);
        return new Enrichment(reactions, savedIds, savedCounts, attachmentsByPost);
    }

    private PostResponse toResponse(Post post, Enrichment e) {
        return PostResponse.from(
                post,
                e.reactions().get(post.getId()),
                e.savedIds().contains(post.getId()),
                e.savedCounts().get(post.getId()),
                toAttachmentResponses(e.attachmentsByPost().getOrDefault(post.getId(), List.of()))
        );
    }

    private List<PostAttachmentResponse> toAttachmentResponses(List<PostAttachment> attachments) {
        return attachments.stream().map(a -> PostAttachmentResponse.from(a, mediaStorageService)).toList();
    }

    private record Enrichment(
            Map<Long, ReactionSummary> reactions,
            Set<Long> savedIds,
            Map<Long, Long> savedCounts,
            Map<Long, List<PostAttachment>> attachmentsByPost
    ) {
    }
}
