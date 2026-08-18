package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.DiseaseGroup;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.SubGroup;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.SubGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.ContentModerationService;
import com.ridvankarsli.sagliktanapi.service.PostAttachmentService;
import com.ridvankarsli.sagliktanapi.service.PostSortOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private PostAttachmentService postAttachmentService;
    // create()/update() artık moderasyondan geçiyor (bkz. PostServiceImpl.
    // moderateOrThrow) - stub'lanmazsa moderate() null döner ve
    // ModerationResult.blocked() çağrısı NPE atar. lenient(): sadece
    // moderasyona ulaşan testlerde kullanılır, forbidden testinde
    // "unnecessary stubbing" uyarısı vermesin diye.
    @Mock
    private ContentModerationService contentModerationService;

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
        lenient().when(contentModerationService.moderate(any()))
                .thenReturn(ContentModerationService.ModerationResult.CLEAN);
    }

    @Test
    void create_throwsForbidden_whenUserNotMemberOfGroup() {
        when(subGroupRepository.findById(SUB_GROUP_ID)).thenReturn(Optional.of(subGroup));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> postService.create(SUB_GROUP_ID, USER_ID, "Başlık", "İçerik", null));

        verify(postRepository, never()).save(any());
    }

    @Test
    void create_savesPost_whenUserIsMember() {
        when(subGroupRepository.findById(SUB_GROUP_ID)).thenReturn(Optional.of(subGroup));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.create(SUB_GROUP_ID, USER_ID, "Başlık", "İçerik", null);

        assertEquals("Başlık", result.getTitle());
        verify(postRepository).save(any(Post.class));
    }

    // Faz 2 adım 4: attachmentKeys geçildiğinde postAttachmentService.attach
    // aynı @Transactional sınırı içinde çağrılmalı - geçersiz bir key tüm
    // post oluşturmayı geri almalı (bkz. PostServiceImpl.create javadoc).
    @Test
    void create_delegatesAttachmentsToPostAttachmentService() {
        when(subGroupRepository.findById(SUB_GROUP_ID)).thenReturn(Optional.of(subGroup));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<String> keys = List.of("posts/abc.jpg");

        postService.create(SUB_GROUP_ID, USER_ID, "Başlık", "İçerik", keys);

        verify(postAttachmentService).attach(any(Post.class), eq(keys));
    }

    // Rapor: küfür/spam içeren başlık/içerik REDDEDİLİR - post hiç kaydedilmez.
    @Test
    void create_throwsBadRequest_whenContentIsBlockedByModeration() {
        when(subGroupRepository.findById(SUB_GROUP_ID)).thenReturn(Optional.of(subGroup));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(contentModerationService.moderate("Küfürlü içerik"))
                .thenReturn(new ContentModerationService.ModerationResult(true, "Uygunsuz içerik", false));

        assertThrows(BadRequestException.class,
                () -> postService.create(SUB_GROUP_ID, USER_ID, "Başlık", "Küfürlü içerik", null));

        verify(postRepository, never()).save(any());
    }

    // Rapor: kriz sinyali (intihar/kendine zarar vb.) İÇERİĞİ ASLA
    // engellemez - sadece flaggedSensitive=true olarak kaydedilir.
    @Test
    void create_savesPostAsFlaggedSensitive_whenModerationDetectsCrisisSignal_butDoesNotBlock() {
        when(subGroupRepository.findById(SUB_GROUP_ID)).thenReturn(Optional.of(subGroup));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(USER_ID, DISEASE_GROUP_ID))
                .thenReturn(true);
        when(contentModerationService.moderate("Kriz içeren içerik"))
                .thenReturn(new ContentModerationService.ModerationResult(false, null, true));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.create(SUB_GROUP_ID, USER_ID, "Başlık", "Kriz içeren içerik", null);

        assertEquals(true, result.isFlaggedSensitive());
        verify(postRepository).save(any(Post.class));
    }

    // Faz 2 adım 1: sort=popular verilince reaksiyon-sayısı sorgusuna,
    // sort=recent (veya belirtilmeyince) mevcut created_at sorgusuna
    // gidilmeli - iki dal birbirini asla tetiklememeli.
    @Test
    void listBySubGroup_usesReactionCountQuery_whenSortIsPopular() {
        Pageable pageable = PageRequest.of(0, 20);
        when(postRepository.findBySubGroupIdOrderByPopularityDesc(eq(SUB_GROUP_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        postService.listBySubGroup(SUB_GROUP_ID, PostSortOption.POPULAR, pageable);

        verify(postRepository).findBySubGroupIdOrderByPopularityDesc(eq(SUB_GROUP_ID), any(Pageable.class));
        verify(postRepository, never()).findBySubGroupIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listBySubGroup_usesCreatedAtQuery_whenSortIsRecent() {
        Pageable pageable = PageRequest.of(0, 20);
        when(postRepository.findBySubGroupIdOrderByCreatedAtDesc(SUB_GROUP_ID, pageable)).thenReturn(Page.empty());

        postService.listBySubGroup(SUB_GROUP_ID, PostSortOption.RECENT, pageable);

        verify(postRepository).findBySubGroupIdOrderByCreatedAtDesc(SUB_GROUP_ID, pageable);
        verify(postRepository, never()).findBySubGroupIdOrderByPopularityDesc(any(), any());
    }

    // Regresyon: canlıda görülen kök neden - Spring'in Pageable resolver'ı
    // "?sort=recent" query param'ını KENDİ Pageable.sort binding'i sanıp
    // Post entity'sinde olmayan "recent" adlı bir alana göre sıralama
    // Sort'u üretiyordu, bu da PropertyReferenceException'a yol açıyordu.
    // Bu test, gelen Pageable'da böyle bir Sort olsa bile repository'e HER
    // ZAMAN sort'u temizlenmiş (unsorted) bir Pageable gittiğini doğruluyor.
    @Test
    void listBySubGroup_stripsIncomingPageableSort_evenIfCallerSuppliesOne() {
        Pageable dirtyPageable = PageRequest.of(0, 20, Sort.by("recent"));
        when(postRepository.findBySubGroupIdOrderByCreatedAtDesc(eq(SUB_GROUP_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        postService.listBySubGroup(SUB_GROUP_ID, PostSortOption.RECENT, dirtyPageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findBySubGroupIdOrderByCreatedAtDesc(eq(SUB_GROUP_ID), captor.capture());
        assertEquals(Sort.unsorted(), captor.getValue().getSort());
    }

    // Faz 2 adım 2: alt gruba özel arama.
    @Test
    void searchBySubGroup_returnsEmptyPage_whenQueryHasNoUsableWords() {
        Page<Post> result = postService.searchBySubGroup(SUB_GROUP_ID, "   ", PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
        verify(postRepository, never()).searchBySubGroup(any(), any(), any(), any());
    }

    @Test
    void searchBySubGroup_delegatesToRepository_withStrippedSort() {
        Pageable dirtyPageable = PageRequest.of(0, 20, Sort.by("recent"));
        when(postRepository.searchBySubGroup(eq(SUB_GROUP_ID), eq("diyabet"), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        postService.searchBySubGroup(SUB_GROUP_ID, "diyabet", dirtyPageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).searchBySubGroup(eq(SUB_GROUP_ID), eq("diyabet"), any(), captor.capture());
        assertEquals(Sort.unsorted(), captor.getValue().getSort());
    }

    // Faz6: sabitlenmiş gönderi - bkz. PostServiceImpl.pin javadoc'u (bilinçli
    // olarak OwnershipGuard.assertOwnerOrAdmin KULLANMIYOR, sadece gerçek
    // sahiplik).
    @Test
    void pin_throwsForbidden_whenRequesterIsNotOwner() {
        Post post = Post.builder().id(1L).user(user).pinned(false).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(ForbiddenException.class, () -> postService.pin(1L, 999L));

        verify(postRepository, never()).save(any());
    }

    @Test
    void pin_unpinsPreviousPinnedPost_beforePinningNewOne() {
        Post previouslyPinned = Post.builder().id(2L).user(user).pinned(true).build();
        Post toPin = Post.builder().id(1L).user(user).pinned(false).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(toPin));
        when(postRepository.findByUserIdAndPinnedTrue(USER_ID)).thenReturn(Optional.of(previouslyPinned));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.pin(1L, USER_ID);

        assertEquals(false, previouslyPinned.isPinned());
        assertEquals(true, result.isPinned());
        verify(postRepository).save(previouslyPinned);
        verify(postRepository).save(toPin);
    }

    @Test
    void unpin_throwsForbidden_whenRequesterIsNotOwner() {
        Post post = Post.builder().id(1L).user(user).pinned(true).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(ForbiddenException.class, () -> postService.unpin(1L, 999L));

        verify(postRepository, never()).save(any());
    }

    @Test
    void unpin_setsPinnedFalse_whenRequesterIsOwner() {
        Post post = Post.builder().id(1L).user(user).pinned(true).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.unpin(1L, USER_ID);

        assertEquals(false, result.isPinned());
    }
}
