package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.ContentReport;
import com.ridvankarsli.sagliktanapi.domain.ReportTargetType;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.ContentReportRepository;
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

    @InjectMocks
    private ContentReportServiceImpl contentReportService;

    private static final Long POST_ID = 1L;
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
}
