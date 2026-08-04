package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.PostAttachment;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.repository.PostAttachmentRepository;
import com.ridvankarsli.sagliktanapi.service.MediaStorageService;
import com.ridvankarsli.sagliktanapi.service.PostAttachmentService;
import com.ridvankarsli.sagliktanapi.util.MediaConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostAttachmentServiceImpl implements PostAttachmentService {

    private final PostAttachmentRepository postAttachmentRepository;
    private final MediaStorageService mediaStorageService;

    @Override
    @Transactional
    public List<PostAttachment> attach(Post post, List<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return List.of();
        }
        if (storageKeys.size() > MediaConstraints.MAX_ATTACHMENTS_PER_POST) {
            throw new BadRequestException(
                    "Bir gönderiye en fazla " + MediaConstraints.MAX_ATTACHMENTS_PER_POST + " fotoğraf eklenebilir");
        }

        List<PostAttachment> toSave = new ArrayList<>(storageKeys.size());
        for (int i = 0; i < storageKeys.size(); i++) {
            String storageKey = storageKeys.get(i);
            if (storageKey == null || storageKey.isBlank()) {
                throw new BadRequestException("Geçersiz fotoğraf referansı");
            }

            // Her key gerçekten R2'ye yüklenmiş mi, tip/boyut beklendiği
            // gibi mi diye doğrulanıyor - presigned URL sadece "buraya
            // yükleyebilirsin" izni verir, ne yüklendiğini garanti etmez.
            MediaStorageService.ObjectMetadata metadata = mediaStorageService.headObject(storageKey)
                    .orElseThrow(() -> new BadRequestException("Yüklenen fotoğraf bulunamadı: " + storageKey));

            if (!MediaConstraints.isAllowedContentType(metadata.contentType())) {
                mediaStorageService.deleteObjects(List.of(storageKey));
                throw new BadRequestException("Desteklenmeyen dosya tipi: " + metadata.contentType());
            }
            if (metadata.contentLength() > MediaConstraints.MAX_FILE_SIZE_BYTES) {
                mediaStorageService.deleteObjects(List.of(storageKey));
                throw new BadRequestException("Dosya çok büyük (maksimum "
                        + (MediaConstraints.MAX_FILE_SIZE_BYTES / (1024 * 1024)) + "MB)");
            }

            toSave.add(PostAttachment.builder()
                    .post(post)
                    .storageKey(storageKey)
                    .sortOrder(i)
                    .build());
        }

        return postAttachmentRepository.saveAll(toSave);
    }

    @Override
    public List<PostAttachment> findByPostId(Long postId) {
        return postAttachmentRepository.findByPostIdOrderBySortOrderAsc(postId);
    }

    @Override
    public Map<Long, List<PostAttachment>> findByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        return postAttachmentRepository.findByPostIdInOrderByPostIdAscSortOrderAsc(postIds).stream()
                .collect(Collectors.groupingBy(
                        pa -> pa.getPost().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Override
    @Transactional
    public void deleteAllForPost(Long postId) {
        List<PostAttachment> existing = postAttachmentRepository.findByPostIdOrderBySortOrderAsc(postId);
        if (existing.isEmpty()) {
            return;
        }
        List<String> storageKeys = existing.stream().map(PostAttachment::getStorageKey).toList();
        mediaStorageService.deleteObjects(storageKeys);
        postAttachmentRepository.deleteByPostId(postId);
    }
}
