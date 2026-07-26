package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.dto.request.CommentRequest;
import com.ridvankarsli.sagliktanapi.dto.response.CommentResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.CommentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CommentRequest request
    ) {
        return CommentResponse.from(commentService.create(postId, principal.getId(), request.content()));
    }

    @GetMapping("/api/posts/{postId}/comments")
    public PageResponse<CommentResponse> listByPost(@PathVariable Long postId, Pageable pageable) {
        return PageResponse.from(commentService.listByPost(postId, pageable).map(CommentResponse::from));
    }

    @PutMapping("/api/comments/{id}")
    public CommentResponse update(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CommentRequest request
    ) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        return CommentResponse.from(commentService.update(id, principal.getId(), isAdmin, request.content()));
    }

    @DeleteMapping("/api/comments/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        commentService.delete(id, principal.getId(), isAdmin);
    }
}
