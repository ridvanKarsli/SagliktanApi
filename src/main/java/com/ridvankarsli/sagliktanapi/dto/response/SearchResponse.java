package com.ridvankarsli.sagliktanapi.dto.response;

import java.util.List;

// Twitter tarzı birleşik "hızlı arama" sonucu: tek sorguda post, yorum ve
// kişi sonuçlarından en alakalı ilk birkaçını bir arada döner (bkz.
// SearchController). Kategori başına tam liste için ayrı /search
// endpoint'leri (posts/search, comments/search, users/search) kullanılır.
public record SearchResponse(
        List<PostResponse> posts,
        List<CommentResponse> comments,
        List<UserSearchResponse> users
) {
}
