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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int PREVIEW_MAX_LENGTH = 200;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ContentReportRepository contentReportRepository;

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
        return userRepository.adminSearch(q, active, roleParam, pageable);
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
    public void resolveReport(Long reportId, Long adminId, ReportStatus newStatus) {
        ContentReport report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Şikayet bulunamadı"));

        report.setStatus(newStatus);
        report.setResolvedBy(userRepository.getReferenceById(adminId));
        report.setResolvedAt(LocalDateTime.now());
        contentReportRepository.save(report);
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
