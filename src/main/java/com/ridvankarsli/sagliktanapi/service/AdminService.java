package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.response.AdminStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    AdminStatsResponse getStats();

    // q/active/role'den herhangi biri null ise o filtre uygulanmaz.
    Page<User> listUsers(String q, Boolean active, Role role, Pageable pageable);

    // requestingAdminId: admin kendi hesabını bu ekrandan pasifleştiremez /
    // yetkisini düşüremez (self-lockout koruması) - bkz. AdminServiceImpl.
    User updateUser(
            Long targetUserId, Long requestingAdminId,
            String firstName, String lastName, String bio, Role role, boolean active
    );

    Page<AdminReportItem> listReports(ReportStatus status, Pageable pageable);

    // deleteContent=true ise, durum güncellemesiyle aynı transaction'da
    // raporlanan post/yorum da silinir (admin'in ownership bypass'ı ile,
    // bkz. PostService/CommentService.delete).
    void resolveReport(Long reportId, Long adminId, ReportStatus newStatus, boolean deleteContent);

    // Genel içerik moderasyonu: sadece şikayet edilenler değil, TÜM
    // postlar/yorumlar. q boşsa tam liste (en yeni önce), doluysa mevcut
    // tam metin arama (bkz. Post/CommentService.search) kullanılır.
    Page<Post> listPosts(String q, Pageable pageable);

    Page<Comment> listComments(String q, Pageable pageable);
}
