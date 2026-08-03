package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.ReactionTargetType;
import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.dto.request.CommentRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ReactionRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ReportRequest;
import com.ridvankarsli.sagliktanapi.dto.response.CommentResponse;
import com.ridvankarsli.sagliktanapi.dto.response.MessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.CommentService;
import com.ridvankarsli.sagliktanapi.service.ContentReportService;
import com.ridvankarsli.sagliktanapi.service.NotificationService;
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
public class CommentController {

    private final CommentService commentService;
    private final ContentReportService contentReportService;
    private final ReactionService reactionService;
    private final NotificationService notificationService;

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CommentRequest request
    ) {
        Comment comment = commentService.create(postId, principal.getId(), request.content(), request.parentCommentId());
        notificationService.notifyNewComment(comment);
        return CommentResponse.from(comment);
    }

    // Sadece üst-seviye yorumlar sayfalanır. Her yorumun alt yanıtları
    // gömülü gelmez; sadece DOĞRUDAN yanıt sayısı (replyCount) döner -
    // kullanıcı bir yorumun yanıtlarını görmek isterse ayrıca
    // GET /api/comments/{commentId}/replies çağrılır (thread-drill).
    @GetMapping("/api/posts/{postId}/comments")
    public PageResponse<CommentResponse> listByPost(
            @PathVariable Long postId, Pageable pageable, @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Page<Comment> topLevelPage = commentService.listByPost(postId, pageable);
        List<Long> ids = topLevelPage.getContent().stream().map(Comment::getId).toList();
        Map<Long, ReactionSummary> reactions =
                reactionService.getSummaries(ReactionTargetType.COMMENT, ids, principal.getId());
        Map<Long, Long> replyCounts = commentService.countReplies(ids);

        return PageResponse.from(topLevelPage.map(comment -> CommentResponse.from(
                comment,
                reactions.getOrDefault(comment.getId(), ReactionSummary.empty()),
                replyCounts.getOrDefault(comment.getId(), 0L)
        )));
    }

    // Bir yorumun DOĞRUDAN yanıtlarını sayfalı döner (thread-drill: X/Twitter
    // tarzı, kullanıcı bir yoruma tıklayınca sadece o yorumun bir alt
    // seviyesi açılır, daha derin yanıtlar kullanıcı ilerledikçe ayrı
    // çağrılarla gelir). Dönen yanıtların da kendi replyCount'u vardır,
    // böylece thread-drill istenildiği kadar derine inebilir.
    @GetMapping("/api/comments/{commentId}/replies")
    public PageResponse<CommentResponse> listReplies(
            @PathVariable Long commentId, Pageable pageable, @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Page<Comment> page = commentService.listReplies(commentId, pageable);
        List<Long> ids = page.getContent().stream().map(Comment::getId).toList();
        Map<Long, ReactionSummary> reactions =
                reactionService.getSummaries(ReactionTargetType.COMMENT, ids, principal.getId());
        Map<Long, Long> replyCounts = commentService.countReplies(ids);

        return PageResponse.from(page.map(comment -> CommentResponse.from(
                comment,
                reactions.getOrDefault(comment.getId(), ReactionSummary.empty()),
                replyCounts.getOrDefault(comment.getId(), 0L)
        )));
    }

    // Gelişmiş arama: yorum içeriğinde tam metin arama (bkz. V7 migration).
    // Sonuçlar düz liste halinde döner, tekrar ağaç kurulmaz - amaç ilgili
    // posta gitmek (postId üzerinden).
    @GetMapping("/api/comments/search")
    public PageResponse<CommentResponse> search(
            @RequestParam String q, Pageable pageable, @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Page<Comment> page = commentService.search(q, pageable);
        List<Long> ids = page.getContent().stream().map(Comment::getId).toList();
        Map<Long, ReactionSummary> reactions =
                reactionService.getSummaries(ReactionTargetType.COMMENT, ids, principal.getId());
        return PageResponse.from(page.map(comment -> CommentResponse.from(comment, reactions.get(comment.getId()))));
    }

    @PutMapping("/api/comments/{id}")
    public CommentResponse update(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CommentRequest request
    ) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        Comment comment = commentService.update(id, principal.getId(), isAdmin, request.content());
        return CommentResponse.from(comment, reactionService.getSummary(ReactionTargetType.COMMENT, id, principal.getId()));
    }

    @DeleteMapping("/api/comments/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        commentService.delete(id, principal.getId(), isAdmin);
    }

    @PostMapping("/api/comments/{id}/report")
    public MessageResponse report(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody(required = false) ReportRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        contentReportService.report(ReportTargetType.COMMENT, id, principal.getId(), reason);
        return new MessageResponse("Şikayetiniz alındı, teşekkür ederiz");
    }

    @PutMapping("/api/comments/{id}/reactions")
    public ReactionSummary react(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ReactionRequest request
    ) {
        reactionService.setReaction(ReactionTargetType.COMMENT, id, principal.getId(), request.value());
        return reactionService.getSummary(ReactionTargetType.COMMENT, id, principal.getId());
    }

    @DeleteMapping("/api/comments/{id}/reactions")
    public ReactionSummary removeReaction(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        reactionService.removeReaction(ReactionTargetType.COMMENT, id, principal.getId());
        return reactionService.getSummary(ReactionTargetType.COMMENT, id, principal.getId());
    }
}
