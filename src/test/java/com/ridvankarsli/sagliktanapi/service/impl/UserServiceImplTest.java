package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.UserDiseaseGroupId;
import com.ridvankarsli.sagliktanapi.dto.response.UserDataExportResponse;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.ReactionRepository;
import com.ridvankarsli.sagliktanapi.repository.RefreshSessionRepository;
import com.ridvankarsli.sagliktanapi.repository.SavedPostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Rapor: hesap silme (anonimleştirme) ve veri dışa aktarma akışlarının
// birim testleri - bkz. UserServiceImpl.deleteAccount/exportData javadoc'u.
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private UserDiseaseGroupRepository userDiseaseGroupRepository;
    @Mock
    private SavedPostRepository savedPostRepository;
    @Mock
    private RefreshSessionRepository refreshSessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private static final Long USER_ID = 1L;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("kullanici@example.com")
                .passwordHash("hashed-current-password")
                .firstName("Ayşe")
                .lastName("Yılmaz")
                .bio("Merhaba, ben Ayşe.")
                .build();
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deleteAccount_throwsBadRequest_whenPasswordIsIncorrect() {
        when(passwordEncoder.matches("yanlış-şifre", user.getPasswordHash())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(USER_ID, "yanlış-şifre"));

        verify(userRepository, never()).save(any());
    }

    // Rapor: hesap silme İÇERİĞİ (post/yorum) silmiyor - sadece kimlik
    // anonimleştiriliyor (bkz. comments.deleted deseniyle aynı gerekçe).
    @Test
    void deleteAccount_anonymizesIdentity_andDeactivates_whenPasswordIsCorrect() {
        when(passwordEncoder.matches("doğru-şifre", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("yeni-rastgele-hash");
        when(userDiseaseGroupRepository.findById_UserId(USER_ID)).thenReturn(List.of());

        userService.deleteAccount(USER_ID, "doğru-şifre");

        assertEquals("Silinmiş", user.getFirstName());
        assertEquals("Kullanıcı", user.getLastName());
        assertEquals("silinmis-" + USER_ID + "@sagliktan.local", user.getEmail());
        assertFalse(user.isActive());
        assertEquals("yeni-rastgele-hash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(savedPostRepository).deleteByUserId(USER_ID);
        // Hesap silinince tüm cihazlardaki oturumlar da geçersiz olmalı (bkz.
        // UserServiceImpl.deleteAccount yorumu, görev #305).
        verify(refreshSessionRepository).deleteByUserId(USER_ID);
    }

    // Rapor: grup üyelikleri İÇERİK değil (bkz. UserServiceImpl.deleteAccount
    // yorumu), hesapla birlikte kaldırılmalı.
    @Test
    void deleteAccount_removesAllGroupMemberships() {
        DiseaseGroup groupA = DiseaseGroup.builder().id(10L).name("Diyabet").build();
        DiseaseGroup groupB = DiseaseGroup.builder().id(20L).name("Astım").build();
        UserDiseaseGroup membershipA = UserDiseaseGroup.builder()
                .id(new UserDiseaseGroupId(USER_ID, 10L)).user(user).diseaseGroup(groupA).build();
        UserDiseaseGroup membershipB = UserDiseaseGroup.builder()
                .id(new UserDiseaseGroupId(USER_ID, 20L)).user(user).diseaseGroup(groupB).build();

        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("yeni-rastgele-hash");
        when(userDiseaseGroupRepository.findById_UserId(USER_ID)).thenReturn(List.of(membershipA, membershipB));

        userService.deleteAccount(USER_ID, "doğru-şifre");

        verify(userDiseaseGroupRepository).deleteById_UserIdAndId_DiseaseGroupId(USER_ID, 10L);
        verify(userDiseaseGroupRepository).deleteById_UserIdAndId_DiseaseGroupId(USER_ID, 20L);
    }

    @Test
    void exportData_includesProfileAndDiseaseGroupNames() {
        DiseaseGroup group = DiseaseGroup.builder().id(10L).name("Diyabet").build();
        UserDiseaseGroup membership = UserDiseaseGroup.builder()
                .id(new UserDiseaseGroupId(USER_ID, 10L)).user(user).diseaseGroup(group).build();

        when(postRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(Page.empty());
        when(commentRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());
        when(userDiseaseGroupRepository.findById_UserId(USER_ID)).thenReturn(List.of(membership));
        when(savedPostRepository.findSavedPostsByUserId(eq(USER_ID), any())).thenReturn(Page.empty());

        UserDataExportResponse export = userService.exportData(USER_ID);

        assertEquals("kullanici@example.com", export.profile().email());
        assertEquals(List.of("Diyabet"), export.diseaseGroups());
        assertEquals(0, export.posts().size());
        assertEquals(0, export.comments().size());
    }
}
