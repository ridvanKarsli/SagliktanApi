package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.ContentReport;
import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.ContentReportRepository;
import com.ridvankarsli.sagliktanapi.repository.MessageRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.ContentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentReportServiceImpl implements ContentReportService {

    private final ContentReportRepository contentReportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    // Faz 2 adım 6: mesaj şikayeti için hedefin gerçekten var olduğunu
    // doğrulamak amacıyla eklendi.
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public void report(ReportTargetType targetType, Long targetId, Long reporterId, String reason) {
        assertTargetExists(targetType, targetId);

        if (contentReportRepository.existsByTargetTypeAndTargetIdAndReporterId(targetType, targetId, reporterId)) {
            return; // zaten şikayet edilmiş, idempotent
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        ContentReport report = ContentReport.builder()
                .targetType(targetType)
                .targetId(targetId)
                .reporter(reporter)
                .reason(reason)
                .build();

        contentReportRepository.save(report);
    }

    private void assertTargetExists(ReportTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            case MESSAGE -> messageRepository.existsById(targetId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(switch (targetType) {
                case POST -> "Gönderi bulunamadı";
                case COMMENT -> "Yorum bulunamadı";
                case MESSAGE -> "Mesaj bulunamadı";
            });
        }
    }
}
