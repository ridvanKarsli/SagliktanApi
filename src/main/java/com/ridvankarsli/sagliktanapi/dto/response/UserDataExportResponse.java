package com.ridvankarsli.sagliktanapi.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// KVKK/6698 sayılı Kanun'un "veri taşınabilirliği" hakkı kapsamında -
// kullanıcı kendi ürettiği veriyi yapılandırılmış (JSON) biçimde
// indirebilir (bkz. UserServiceImpl.exportData / UserController.exportMyData).
// Sadece kendi verisi taşınır; başka kullanıcılara ait içerik (ör. kaydettiği
// gönderilerin yazarı) dahil edilmez.
public record UserDataExportResponse(
        ProfileData profile,
        List<PostData> posts,
        List<CommentData> comments,
        List<String> diseaseGroups,
        List<SavedPostRef> savedPosts,
        LocalDateTime exportedAt
) {
    public record ProfileData(
            Long id, String email, String firstName, String lastName, String bio, LocalDateTime createdAt
    ) {
    }

    public record PostData(Long id, String title, String content, LocalDateTime createdAt) {
    }

    public record CommentData(Long id, String content, boolean deleted, LocalDateTime createdAt) {
    }

    // Kaydedilen gönderinin kendisi başkasına ait olabileceği için sadece
    // referans (id + başlık) taşınır, o gönderinin tam içeriği/yazarı değil.
    public record SavedPostRef(Long postId, String title) {
    }
}
