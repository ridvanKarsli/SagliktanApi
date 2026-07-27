package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
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
    void create_flattensReplyToReply_toTopLevelAncestor() {
        Comment topLevelComment = Comment.builder().id(50L).post(post).user(user).content("Ana yorum").build();
        Comment firstReply = Comment.builder().id(51L).post(post).user(user).content("İlk yanıt")
                .parentComment(topLevelComment).build();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(commentRepository.findById(51L)).thenReturn(Optional.of(firstReply));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Bir yanıta yanıt verilirse (51L'e), yeni yorum ikinci seviyede
        // kalmasın diye otomatik olarak en üstteki yoruma (50L) bağlanmalı.
        Comment replyToReply = commentService.create(POST_ID, USER_ID, "Yanıta yanıt", 51L);

        assertEquals(topLevelComment, replyToReply.getParentComment());
    }
}
