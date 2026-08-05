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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentReportServiceImplTest {

    @Mock
    private ContentReportRepository contentReportRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ContentReportServiceImpl contentReportService;

    private static final Long POST_ID = 1L;
    private static final Long MESSAGE_ID = 3L;
    private static final Long REPORTER_ID = 2L;

    @Test
    void report_throwsNotFound_whenPostDoesNotExist() {
        when(postRepository.existsById(POST_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> contentReportService.report(ReportTargetType.POST, POST_ID, REPORTER_ID, "Uygunsuz içerik"));

        verify(contentReportRepository, never()).save(any());
    }

    @Test
    void report_isIdempotent_whenAlreadyReportedBySameUser() {
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(contentReportRepository.existsByTargetTypeAndTargetIdAndReporterId(
                ReportTargetType.POST, POST_ID, REPORTER_ID)).thenReturn(true);

        contentReportService.report(ReportTargetType.POST, POST_ID, REPORTER_ID, "Uygunsuz içerik");

        verify(contentReportRepository, never()).save(any());
    }

    @Test
    void report_savesReport_whenValidAndNotAlreadyReported() {
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(contentReportRepository.existsByTargetTypeAndTargetIdAndReporterId(
                ReportTargetType.POST, POST_ID, REPORTER_ID)).thenReturn(false);
        when(userRepository.findById(REPORTER_ID))
                .thenReturn(Optional.of(User.builder().id(REPORTER_ID).build()));

        contentReportService.report(ReportTargetType.POST, POST_ID, REPORTER_ID, "Uygunsuz içerik");

        verify(contentReportRepository).save(any(ContentReport.class));
    }

    // Faz 2 adım 6: mesaj şikayeti de POST/COMMENT ile aynı doğrulama
    // akışından geçmeli - assertTargetExists artık MessageRepository'yi de
    // kontrol ediyor (bkz. ContentReportServiceImpl).
    @Test
    void report_savesReport_whenMessageTarget() {
        when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
        when(contentReportRepository.existsByTargetTypeAndTargetIdAndReporterId(
                ReportTargetType.MESSAGE, MESSAGE_ID, REPORTER_ID)).thenReturn(false);
        when(userRepository.findById(REPORTER_ID))
                .thenReturn(Optional.of(User.builder().id(REPORTER_ID).build()));

        contentReportService.report(ReportTargetType.MESSAGE, MESSAGE_ID, REPORTER_ID, "Rahatsız edici mesaj");

        verify(contentReportRepository).save(any(ContentReport.class));
    }

    @Test
    void report_throwsNotFound_whenMessageDoesNotExist() {
        when(messageRepository.existsById(MESSAGE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> contentReportService.report(ReportTargetType.MESSAGE, MESSAGE_ID, REPORTER_ID, "Rahatsız edici mesaj"));

        verify(contentReportRepository, never()).save(any());
    }
}
