package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.dto.request.PostRequest;
import com.ridvankarsli.sagliktanapi.dto.request.ReportRequest;
import com.ridvankarsli.sagliktanapi.dto.response.MessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PostResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.ContentReportService;
import com.ridvankarsli.sagliktanapi.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ContentReportService contentReportService;

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

    @GetMapping("/api/sub-groups/{subGroupId}/posts")
    public PageResponse<PostResponse> listBySubGroup(@PathVariable Long subGroupId, Pageable pageable) {
        return PageResponse.from(postService.listBySubGroup(subGroupId, pageable).map(PostResponse::from));
    }

    // Rapor 4.5: PostgreSQL Full-Text Search
    @GetMapping("/api/posts/search")
    public PageResponse<PostResponse> search(@RequestParam String q, Pageable pageable) {
        return PageResponse.from(postService.search(q, pageable).map(PostResponse::from));
    }

    @GetMapping("/api/posts/{id}")
    public PostResponse getById(@PathVariable Long id) {
        return PostResponse.from(postService.getById(id));
    }

    @PutMapping("/api/posts/{id}")
    public PostResponse update(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PostRequest request
    ) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        return PostResponse.from(
                postService.update(id, principal.getId(), isAdmin, request.title(), request.content())
        );
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
}
