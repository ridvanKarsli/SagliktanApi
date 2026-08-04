package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.SavedPostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Faz 2 adım 3: yıldızlama servisi - DB gerektirmeyen birim testler.
@ExtendWith(MockitoExtension.class)
class SavedPostServiceImplTest {

    @Mock
    private SavedPostRepository savedPostRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SavedPostServiceImpl savedPostService;

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;

    @Test
    void save_isNoOp_whenAlreadySaved() {
        when(savedPostRepository.existsByUserIdAndPostId(USER_ID, POST_ID)).thenReturn(true);

        savedPostService.save(USER_ID, POST_ID);

        verify(savedPostRepository, never()).save(any());
    }

    @Test
    void save_throwsNotFound_whenPostDoesNotExist() {
        when(savedPostRepository.existsByUserIdAndPostId(USER_ID, POST_ID)).thenReturn(false);
        when(postRepository.existsById(POST_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> savedPostService.save(USER_ID, POST_ID));

        verify(savedPostRepository, never()).save(any());
    }

    @Test
    void save_persists_whenNotAlreadySavedAndPostExists() {
        when(savedPostRepository.existsByUserIdAndPostId(USER_ID, POST_ID)).thenReturn(false);
        when(postRepository.existsById(POST_ID)).thenReturn(true);

        savedPostService.save(USER_ID, POST_ID);

        verify(savedPostRepository).save(any());
    }

    @Test
    void unsave_delegatesToDerivedDelete() {
        savedPostService.unsave(USER_ID, POST_ID);

        verify(savedPostRepository).deleteByUserIdAndPostId(USER_ID, POST_ID);
    }

    @Test
    void findSavedPostIds_returnsEmptySet_whenUserIdIsNull() {
        Set<Long> result = savedPostService.findSavedPostIds(null, List.of(POST_ID));

        assertTrue(result.isEmpty());
        verify(savedPostRepository, never()).findSavedPostIds(any(), any());
    }

    @Test
    void findSavedPostIds_returnsEmptySet_whenPostIdsIsEmpty() {
        Set<Long> result = savedPostService.findSavedPostIds(USER_ID, List.of());

        assertTrue(result.isEmpty());
        verify(savedPostRepository, never()).findSavedPostIds(any(), any());
    }

    @Test
    void findSavedPostIds_delegatesToRepository() {
        when(savedPostRepository.findSavedPostIds(USER_ID, List.of(POST_ID))).thenReturn(List.of(POST_ID));

        Set<Long> result = savedPostService.findSavedPostIds(USER_ID, List.of(POST_ID));

        assertEquals(Set.of(POST_ID), result);
    }

    @Test
    void isSaved_returnsFalse_whenUserIdIsNull() {
        assertFalse(savedPostService.isSaved(null, POST_ID));
        verify(savedPostRepository, never()).existsByUserIdAndPostId(any(), any());
    }

    @Test
    void countByPostIds_returnsEmptyMap_whenPostIdsIsEmpty() {
        Map<Long, Long> result = savedPostService.countByPostIds(List.of());

        assertTrue(result.isEmpty());
        verify(savedPostRepository, never()).countGrouped(any());
    }

    @Test
    void countByPostIds_defaultsToZero_forPostsWithNoSaves() {
        Long otherPostId = 20L;
        when(savedPostRepository.countGrouped(List.of(POST_ID, otherPostId)))
                .thenReturn(List.of(row(POST_ID, 3L)));

        Map<Long, Long> result = savedPostService.countByPostIds(List.of(POST_ID, otherPostId));

        assertEquals(3L, result.get(POST_ID));
        assertEquals(0L, result.get(otherPostId));
    }

    private static SavedPostRepository.SavedPostCountRow row(Long postId, long count) {
        return new SavedPostRepository.SavedPostCountRow() {
            @Override
            public Long getPostId() { return postId; }
            @Override
            public long getCount() { return count; }
        };
    }

    // Regresyon: Faz 2 adım 1'de yaşanan "Pageable'ın otomatik sort binding'i
    // özel sorgunun kendi ORDER BY'ıyla çakışıyor" hatasının burada da
    // yaşanmadığını doğrular.
    @Test
    void listSavedByUser_stripsIncomingPageableSort() {
        Pageable dirtyPageable = PageRequest.of(0, 20, Sort.by("whatever"));
        when(savedPostRepository.findSavedPostsByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        savedPostService.listSavedByUser(USER_ID, dirtyPageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(savedPostRepository).findSavedPostsByUserId(eq(USER_ID), captor.capture());
        assertEquals(Sort.unsorted(), captor.getValue().getSort());
    }
}
