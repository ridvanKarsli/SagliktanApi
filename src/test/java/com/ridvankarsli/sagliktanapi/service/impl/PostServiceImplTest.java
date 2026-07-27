package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.SubGroupRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Rapor: grup üyeliği kontrolünün (bkz. PostServiceImpl.assertMemberOfGroup)
// gerçekten devrede olduğunu doğrulayan birim testler - DB gerektirmez.
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private SubGroupRepository subGroupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDiseaseGroupRepository userDiseaseGroupRepository;

    @InjectMocks
    private PostServiceImpl postService;

    private static final Long USER_ID = 1L;
    private static final Long SUB_GROUP_ID = 10L;
    private static final Long DISEASE_GROUP_ID = 100L;

    private SubGroup subGroup;
    private User user;

    @BeforeEach
    void setUp() {
        DiseaseGroup diseaseGroup = DiseaseGroup.builder().id(DISEASE_GROUP_ID).name("Retinitis Pigmentosa").build();
        subGroup = SubGroup.builder().id(SUB_GROUP_ID).diseaseGroup(diseaseGroup).name("Sohbet").build();
        user = User.builder().id(USER_ID).email("test@example.com").build();
    }

    @Test
    void create_throwsForbidden_whenUserNotMemberOfGroup() {
        when(subGroupRepository.findById(SUB_GROUP_ID)).thenReturn(Optional.of(subGroup));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> postService.create(SUB_GROUP_ID, USER_ID, "Başlık", "İçerik"));

        verify(postRepository, never()).save(any());
    }

    @Test
    void create_savesPost_whenUserIsMember() {
        when(subGroupRepository.findById(SUB_GROUP_ID)).thenReturn(Optional.of(subGroup));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.create(SUB_GROUP_ID, USER_ID, "Başlık", "İçerik");

        assertEquals("Başlık", result.getTitle());
        verify(postRepository).save(any(Post.class));
    }
}
