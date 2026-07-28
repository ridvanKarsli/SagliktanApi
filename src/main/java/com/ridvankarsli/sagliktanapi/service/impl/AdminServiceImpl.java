package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.ContentReport;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.ReportStatus;
import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.domain.Role;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.dto.response.AdminStatsResponse;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.ContentReportRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.AdminReportItem;
import com.ridvankarsli.sagliktanapi.service.AdminService;
import com.ridvankarsli.sagliktanapi.service.CommentService;
import com.ridvankarsli.sagliktanapi.service.PostService;
import com.ridvankarsli.sagliktanapi.util.SearchQueryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int PREVIEW_MAX_LENGTH = 200;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ContentReportRepository contentReportRepository;
    // Post/CommentService (repository değil) kasıtlı kullanılıyor: silme
    // işlemleri owner-or-admin kontrolünden ve (yorum için) soft-delete
    // mantığından geçmeli - bunları burada tekrar yazmak yerine mevcut,
    // zaten denetimden geçmiş service metotları çağrılıyor.
    private final PostService postService;
    private final CommentService commentService;

    @Override
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                postRepository.count(),
                commentRepository.count(),
                contentReportRepository.countByStatus(ReportStatus.PENDING)
        );
    }

    @Override
    public Page<User> listUsers(String q, Boolean active, Role role, Pageable pageable) {
        String roleParam = role != null ? role.name() : null;
        // adminSearch native query'sinin kendi ORDER BY u.created_at DESC'i
        // var - client bir sort parametresi gönderirse (bkz. listPosts/
        // listComments'teki aynı sınıf hata, SearchQueryUtil.stripSort
        // dokümantasyonu) çift ORDER BY SQL syntax hatasına düşer.
        return userRepository.adminSearch(q, active, roleParam, SearchQueryUtil.stripSort(pageable));
    }

    @Override
    @Transactional
    public User updateUser(
            Long targetUserId, Long requestingAdminId,
            String firstName, String lastName, String bio, Role role, boolean active
    ) {
        // Self-lockout koruması: admin kendi hesabını bu ekrandan
        // pasifleştirip ya da yetkisini düşürüp panelden kendini dışarıda
        // bırakamaz - profilini (ad/soyad/bio) yine düzenleyebilir.
        if (targetUserId.equals(requestingAdminId) && (role != Role.ADMIN || !active)) {
            throw new BadRequestException("Kendi hesabınızın rolünü ya da aktiflik durumunu bu ekrandan değiştiremezsiniz");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBio(bio);
        user.setRole(role);
        user.setActive(active);

        return userRepository.save(user);
    }

    @Override
    public Page<AdminReportItem> listReports(ReportStatus status, Pageable pageable) {
        Page<ContentReport> page = status == null
                ? contentReportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : contentReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return page.map(this::toItem);
    }

    @Override
    @Transactional
    public void resolveReport(Long reportId, Long adminId, ReportStatus newStatus, boolean deleteContent) {
        ContentReport report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Şikayet bulunamadı"));

        // İçerik silme, durum güncellemesiyle AYNI transaction'da yapılır -
        // silme başarısız olursa (ör. içerik zaten silinmiş) durum
        // güncellemesi de rollback olur, admin tekrar deneyebilir.
        if (deleteContent) {
            if (report.getTargetType() == ReportTargetType.POST) {
                postService.delete(report.getTargetId(), adminId, true);
            } else {
                commentService.delete(report.getTargetId(), adminId, true);
            }
        }

        report.setStatus(newStatus);
        report.setResolvedBy(userRepository.getReferenceById(adminId));
        report.setResolvedAt(LocalDateTime.now());
        contentReportRepository.save(report);
    }

    @Override
    public Page<Post> listPosts(String q, Pageable pageable) {
        return StringUtils.hasText(q) ? postService.search(q, pageable) : postRepository.findAll(pageable);
    }

    @Override
    public Page<Comment> listComments(String q, Pageable pageable) {
        return StringUtils.hasText(q) ? commentService.search(q, pageable) : commentRepository.findAll(pageable);
    }

    private AdminReportItem toItem(ContentReport report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            return postRepository.findById(report.getTargetId())
                    .map(p -> new AdminReportItem(
                            report,
                            truncate(p.getTitle() + " — " + p.getContent()),
                            p.getUser().getId(),
                            p.getUser().getFirstName() + " " + p.getUser().getLastName()
                    ))
                    .orElseGet(() -> new AdminReportItem(report, "[Gönderi silinmiş]", null, null));
        }

        return commentRepository.findById(report.getTargetId())
                .map(c -> new AdminReportItem(
                        report,
                        truncate(c.getContent()),
                        c.getUser().getId(),
                        c.getUser().getFirstName() + " " + c.getUser().getLastName()
                ))
                .orElseGet(() -> new AdminReportItem(report, "[Yorum silinmiş]", null, null));
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > PREVIEW_MAX_LENGTH ? text.substring(0, PREVIEW_MAX_LENGTH) + "…" : text;
    }
}
