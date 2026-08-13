package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.ContentModerationService;
import com.ridvankarsli.sagliktanapi.service.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Rapor: yorum yaparken de grup üyeliği kontrolünün (bkz.
// CommentServiceImpl.assertMemberOfGroup) devrede olduğunu doğrular.
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDiseaseGroupRepository userDiseaseGroupRepository;
    // clean-code denetimi sonrası CommentServiceImpl kendi assertOwnerOrAdmin
    // metodunu kaldırıp paylaşılan OwnershipGuard'a taşıdı (bkz.
    // service/OwnershipGuard.java) - constructor artık bunu da istiyor,
    // yoksa @InjectMocks alanı null bırakıyor ve testler NPE ile patlıyordu.
    @Mock
    private OwnershipGuard ownershipGuard;
    // create()/update() artık moderasyondan geçiyor (bkz.
    // CommentServiceImpl.moderateOrThrow) - bkz. PostServiceImplTest'teki
    // aynı gerekçe.
    @Mock
    private ContentModerationService contentModerationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 5L;
    private static final Long DISEASE_GROUP_ID = 100L;

    private Post post;
    private User user;

    @BeforeEach
    void setUp() {
        DiseaseGroup diseaseGroup = DiseaseGroup.builder().id(DISEASE_GROUP_ID).name("Retinitis Pigmentosa").build();
        SubGroup subGroup = SubGroup.builder().id(10L).diseaseGroup(diseaseGroup).name("Sohbet").build();
        post = Post.builder().id(POST_ID).subGroup(subGroup).title("Başlık").content("İçerik").build();
        user = User.builder().id(USER_ID).email("test@example.com").build();
        lenient().when(contentModerationService.moderate(any()))
                .thenReturn(ContentModerationService.ModerationResult.CLEAN);
    }

    @Test
    void create_throwsForbidden_whenUserNotMemberOfGroup() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> commentService.create(POST_ID, USER_ID, "Yorum", null));

        verify(commentRepository, never()).save(any());
    }

    @Test
    void create_savesTopLevelComment_whenUserIsMember() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.create(POST_ID, USER_ID, "Yorum", null);

        assertNull(result.getParentComment());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void create_savesReply_attachedToParent_whenParentIsTopLevel() {
        Comment topLevelComment = Comment.builder().id(50L).post(post).user(user).content("Ana yorum").build();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(commentRepository.findById(50L)).thenReturn(Optional.of(topLevelComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment reply = commentService.create(POST_ID, USER_ID, "Yanıt", 50L);

        assertEquals(topLevelComment, reply.getParentComment());
    }

    @Test
    void create_keepsRealParent_whenReplyingToAReply() {
        Comment topLevelComment = Comment.builder().id(50L).post(post).user(user).content("Ana yorum").build();
        Comment firstReply = Comment.builder().id(51L).post(post).user(user).content("İlk yanıt")
                .parentComment(topLevelComment).build();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(commentRepository.findById(51L)).thenReturn(Optional.of(firstReply));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Bir yanıta yanıt verilirse (51L'e), derinlik sınırsız olduğu için
        // artık en üstteki yoruma değil, doğrudan yanıtlanan yoruma (51L)
        // bağlanmalı.
        Comment replyToReply = commentService.create(POST_ID, USER_ID, "Yanıta yanıt", 51L);

        assertEquals(firstReply, replyToReply.getParentComment());
    }

    @Test
    void delete_marksCommentAsDeleted_insteadOfRemovingRow() {
        Comment comment = Comment.builder().id(50L).post(post).user(user).content("Ana yorum").build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.delete(50L, USER_ID, false);

        assertEquals(true, comment.isDeleted());
        verify(commentRepository, never()).delete(any(Comment.class));
        verify(commentRepository).save(comment);
    }

    @Test
    void update_throwsBadRequest_whenCommentAlreadyDeleted() {
        Comment comment = Comment.builder().id(50L).post(post).user(user).content("Ana yorum")
                .deleted(true).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        assertThrows(BadRequestException.class,
                () -> commentService.update(50L, USER_ID, false, "Düzenlenmiş içerik"));

        verify(commentRepository, never()).save(any());
    }

    // Rapor: küfür/spam içeren yorum REDDEDİLİR - hiç kaydedilmez.
    @Test
    void create_throwsBadRequest_whenContentIsBlockedByModeration() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(contentModerationService.moderate("Küfürlü yorum"))
                .thenReturn(new ContentModerationService.ModerationResult(true, "Uygunsuz içerik", false));

        assertThrows(BadRequestException.class,
                () -> commentService.create(POST_ID, USER_ID, "Küfürlü yorum", null));

        verify(commentRepository, never()).save(any());
    }

    // Rapor: kriz sinyali İÇERİĞİ ASLA engellemez - sadece
    // flaggedSensitive=true olarak kaydedilir (bkz. ContentModerationService
    // javadoc'u - kimseyi zor bir deneyimini paylaşırken susturmuyoruz).
    @Test
    void create_savesCommentAsFlaggedSensitive_whenModerationDetectsCrisisSignal_butDoesNotBlock() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(contentModerationService.moderate("Kriz içeren yorum"))
                .thenReturn(new ContentModerationService.ModerationResult(false, null, true));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.create(POST_ID, USER_ID, "Kriz içeren yorum", null);

        assertEquals(true, result.isFlaggedSensitive());
    }
}
