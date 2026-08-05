package com.ridvankarsli.sagliktanapi.controller;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.PostAttachment;
import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.dto.request.AdminReportActionRequest;
import com.ridvankarsli.sagliktanapi.dto.request.AdminUserUpdateRequest;
import com.ridvankarsli.sagliktanapi.dto.response.AdminCommentResponse;
import com.ridvankarsli.sagliktanapi.dto.response.AdminPostResponse;
import com.ridvankarsli.sagliktanapi.dto.response.AdminReportResponse;
import com.ridvankarsli.sagliktanapi.dto.response.AdminStatsResponse;
import com.ridvankarsli.sagliktanapi.dto.response.AdminUserResponse;
import com.ridvankarsli.sagliktanapi.dto.response.MessageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PageResponse;
import com.ridvankarsli.sagliktanapi.dto.response.PostAttachmentResponse;
import com.ridvankarsli.sagliktanapi.security.CustomUserDetails;
import com.ridvankarsli.sagliktanapi.service.AdminService;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import com.ridvankarsli.sagliktanapi.service.PostAttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Tüm admin paneli uçları burada toplanıyor. Sınıf seviyesindeki
// @PreAuthorize, SecurityConfig'deki genel authenticated() kuralının
// üzerine ikinci bir savunma katmanı ekliyor - bir route eşleşme hatası
// olsa bile method security bağımsız olarak reddeder.
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    // Fotoğrafları toplu (N+1 sorgu değil) çekip admin listesine
    // zenginleştirmek için - bkz. PostResponseAssembler'daki aynı desen.
    private final PostAttachmentService postAttachmentService;
    private final MediaStorageService mediaStorageService;

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.getStats();
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Role role,
            Pageable pageable
    ) {
        return PageResponse.from(adminService.listUsers(q, active, role, pageable).map(AdminUserResponse::from));
    }

    @PutMapping("/users/{id}")
    public AdminUserResponse updateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return AdminUserResponse.from(adminService.updateUser(
                id, principal.getId(), request.firstName(), request.lastName(), request.bio(),
                request.role(), request.active()
        ));
    }

    @GetMapping("/reports")
    public PageResponse<AdminReportResponse> listReports(
            @RequestParam(required = false) ReportStatus status, Pageable pageable
    ) {
        return PageResponse.from(adminService.listReports(status, pageable).map(AdminReportResponse::from));
    }

    @PutMapping("/reports/{id}")
    public MessageResponse resolveReport(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AdminReportActionRequest request
    ) {
        adminService.resolveReport(id, principal.getId(), request.status(), request.deleteContent());
        return new MessageResponse(request.deleteContent() ? "İçerik silindi, şikayet güncellendi" : "Şikayet güncellendi");
    }

    // Genel içerik moderasyonu: sadece şikayet edilenler değil TÜM
    // postlar/yorumlar - silme işlemi için mevcut DELETE /api/posts/{id} ve
    // DELETE /api/comments/{id} uçları kullanılır (admin zaten ownership
    // bypass'ına sahip, burada tekrar yazılmadı).
    // hasPhotos=true: tehlikeli/uygunsuz görsel içerik denetimi için sadece
    // fotoğraflı gönderileri listeler (bkz. AdminService.listPosts).
    @GetMapping("/posts")
    public PageResponse<AdminPostResponse> listPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean hasPhotos,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Post> page = adminService.listPosts(q, hasPhotos, pageable);
        List<Long> ids = page.getContent().stream().map(Post::getId).toList();
        Map<Long, List<PostAttachment>> attachmentsByPost = postAttachmentService.findByPostIds(ids);
        return PageResponse.from(page.map(post -> AdminPostResponse.from(
                post, toAttachmentResponses(attachmentsByPost.getOrDefault(post.getId(), List.of())))));
    }

    @GetMapping("/comments")
    public PageResponse<AdminCommentResponse> listComments(
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return PageResponse.from(adminService.listComments(q, pageable).map(AdminCommentResponse::from));
    }

    private List<PostAttachmentResponse> toAttachmentResponses(List<PostAttachment> attachments) {
        return attachments.stream().map(a -> PostAttachmentResponse.from(a, mediaStorageService)).toList();
    }
}
