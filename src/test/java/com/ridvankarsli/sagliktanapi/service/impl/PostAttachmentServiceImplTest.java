package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.PostAttachment;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.repository.PostAttachmentRepository;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Faz 2 adım 4: gönderiye fotoğraf ekleme servisi - DB/R2 gerektirmeyen
// birim testler (bkz. SavedPostServiceImplTest ile aynı desen).
@ExtendWith(MockitoExtension.class)
class PostAttachmentServiceImplTest {

    @Mock
    private PostAttachmentRepository postAttachmentRepository;
    @Mock
    private MediaStorageService mediaStorageService;

    @InjectMocks
    private PostAttachmentServiceImpl postAttachmentService;

    private static final Long POST_ID = 10L;
    private static final String KEY = "posts/abc.jpg";

    private static Post post() {
        return Post.builder().id(POST_ID).build();
    }

    @Test
    void attach_returnsEmptyList_whenStorageKeysIsNull() {
        List<PostAttachment> result = postAttachmentService.attach(post(), null);

        assertTrue(result.isEmpty());
        verify(postAttachmentRepository, never()).saveAll(anyList());
    }

    @Test
    void attach_returnsEmptyList_whenStorageKeysIsEmpty() {
        List<PostAttachment> result = postAttachmentService.attach(post(), List.of());

        assertTrue(result.isEmpty());
        verify(postAttachmentRepository, never()).saveAll(anyList());
    }

    @Test
    void attach_throwsBadRequest_whenTooManyKeys() {
        List<String> keys = List.of("1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg", "7.jpg");

        assertThrows(BadRequestException.class, () -> postAttachmentService.attach(post(), keys));
        verify(mediaStorageService, never()).headObject(any());
    }

    @Test
    void attach_throwsBadRequest_whenKeyIsBlank() {
        assertThrows(BadRequestException.class, () -> postAttachmentService.attach(post(), List.of("  ")));
    }

    @Test
    void attach_throwsBadRequest_whenObjectNotFoundInStorage() {
        when(mediaStorageService.headObject(KEY)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> postAttachmentService.attach(post(), List.of(KEY)));
        verify(postAttachmentRepository, never()).saveAll(anyList());
    }

    @Test
    void attach_throwsBadRequest_andDeletesObject_whenContentTypeNotAllowed() {
        when(mediaStorageService.headObject(KEY))
                .thenReturn(Optional.of(new MediaStorageService.ObjectMetadata("application/pdf", 1024)));

        assertThrows(BadRequestException.class, () -> postAttachmentService.attach(post(), List.of(KEY)));
        verify(mediaStorageService).deleteObjects(List.of(KEY));
    }

    @Test
    void attach_throwsBadRequest_andDeletesObject_whenFileTooLarge() {
        long tooLarge = 9L * 1024 * 1024;
        when(mediaStorageService.headObject(KEY))
                .thenReturn(Optional.of(new MediaStorageService.ObjectMetadata("image/jpeg", tooLarge)));

        assertThrows(BadRequestException.class, () -> postAttachmentService.attach(post(), List.of(KEY)));
        verify(mediaStorageService).deleteObjects(List.of(KEY));
    }

    @Test
    void attach_savesAttachmentsWithSortOrder_whenAllValid() {
        Post post = post();
        List<String> keys = List.of("posts/a.jpg", "posts/b.png");
        when(mediaStorageService.headObject("posts/a.jpg"))
                .thenReturn(Optional.of(new MediaStorageService.ObjectMetadata("image/jpeg", 1024)));
        when(mediaStorageService.headObject("posts/b.png"))
                .thenReturn(Optional.of(new MediaStorageService.ObjectMetadata("image/png", 2048)));
        when(postAttachmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PostAttachment> result = postAttachmentService.attach(post, keys);

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getSortOrder());
        assertEquals(1, result.get(1).getSortOrder());
        assertEquals("posts/a.jpg", result.get(0).getStorageKey());
    }

    @Test
    void findByPostIds_returnsEmptyMap_whenPostIdsIsEmpty() {
        Map<Long, List<PostAttachment>> result = postAttachmentService.findByPostIds(List.of());

        assertTrue(result.isEmpty());
        verify(postAttachmentRepository, never()).findByPostIdInOrderByPostIdAscSortOrderAsc(any());
    }

    @Test
    void findByPostIds_groupsAttachmentsByPostId() {
        Post post = post();
        PostAttachment a1 = PostAttachment.builder().id(1L).post(post).storageKey("a").sortOrder(0).build();
        PostAttachment a2 = PostAttachment.builder().id(2L).post(post).storageKey("b").sortOrder(1).build();
        when(postAttachmentRepository.findByPostIdInOrderByPostIdAscSortOrderAsc(List.of(POST_ID)))
                .thenReturn(List.of(a1, a2));

        Map<Long, List<PostAttachment>> result = postAttachmentService.findByPostIds(List.of(POST_ID));

        assertEquals(2, result.get(POST_ID).size());
    }

    @Test
    void deleteAllForPost_isNoOp_whenNoAttachments() {
        when(postAttachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).thenReturn(List.of());

        postAttachmentService.deleteAllForPost(POST_ID);

        verify(mediaStorageService, never()).deleteObjects(any());
        verify(postAttachmentRepository, never()).deleteByPostId(any());
    }

    @Test
    void deleteAllForPost_deletesStorageObjectsAndDbRows_whenAttachmentsExist() {
        PostAttachment attachment = PostAttachment.builder().id(1L).post(post()).storageKey(KEY).sortOrder(0).build();
        when(postAttachmentRepository.findByPostIdOrderBySortOrderAsc(POST_ID)).thenReturn(List.of(attachment));

        postAttachmentService.deleteAllForPost(POST_ID);

        verify(mediaStorageService).deleteObjects(List.of(KEY));
        verify(postAttachmentRepository).deleteByPostId(POST_ID);
    }
}
