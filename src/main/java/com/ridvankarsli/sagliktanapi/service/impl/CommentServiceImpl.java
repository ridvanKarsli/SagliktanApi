package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Comment;
import com.ridvankarsli.sagliktanapi.domain.Post;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.BadRequestException;
import com.ridvankarsli.sagliktanapi.exception.ForbiddenException;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.UserDiseaseGroupRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.CommentService;
import com.ridvankarsli.sagliktanapi.service.ContentModerationService;
import com.ridvankarsli.sagliktanapi.service.OwnershipGuard;
import com.ridvankarsli.sagliktanapi.util.SearchQueryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserDiseaseGroupRepository userDiseaseGroupRepository;
    private final OwnershipGuard ownershipGuard;
    private final ContentModerationService contentModerationService;

    @Override
    @Transactional
    public Comment create(Long postId, Long userId, String content, Long parentCommentId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Gönderi bulunamadı"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        assertMemberOfGroup(userId, post.getSubGroup().getDiseaseGroup().getId());

        Comment parent = resolveParent(parentCommentId, postId);
        boolean sensitive = moderateOrThrow(content);

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .parentComment(parent)
                .flaggedSensitive(sensitive)
                .build();

        return commentRepository.save(comment);
    }

    @Override
    public Page<Comment> listByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtAsc(postId, pageable);
    }

    @Override
    public Page<Comment> listReplies(Long parentCommentId, Pageable pageable) {
        return commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId, pageable);
    }

    @Override
    public Map<Long, Long> countReplies(Collection<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Long id : parentIds) {
            counts.put(id, 0L);
        }
        for (CommentRepository.ReplyCountRow row : commentRepository.countRepliesGrouped(parentIds)) {
            counts.put(row.getParentId(), row.getCount());
        }
        return counts;
    }

    @Override
    public Page<Comment> search(String query, Pageable pageable) {
        String tsQuery = SearchQueryUtil.toPrefixTsQuery(query);
        if (tsQuery == null) {
            return Page.empty(pageable);
        }
        return commentRepository.search(query, tsQuery, SearchQueryUtil.stripSort(pageable));
    }

    @Override
    @Transactional
    public Comment update(Long commentId, Long requesterId, boolean requesterIsAdmin, String content) {
        Comment comment = getById(commentId);
        ownershipGuard.assertOwnerOrAdmin(comment.getUser().getId(), requesterId, requesterIsAdmin);
        if (comment.isDeleted()) {
            throw new BadRequestException("Silinmiş bir yorum düzenlenemez");
        }
        boolean sensitive = moderateOrThrow(content);
        comment.setContent(content);
        comment.setFlaggedSensitive(sensitive);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long requesterId, boolean requesterIsAdmin) {
        Comment comment = getById(commentId);
        ownershipGuard.assertOwnerOrAdmin(comment.getUser().getId(), requesterId, requesterIsAdmin);
        if (comment.isDeleted()) {
            throw new BadRequestException("Bu yorum zaten silinmiş");
        }
        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    private Comment getById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı"));
    }

    // Kullanıcı, yorum yapacağı gönderinin bağlı olduğu hastalık grubuna üye
    // değilse işlem reddedilir.
    private void assertMemberOfGroup(Long userId, Long diseaseGroupId) {
        if (!userDiseaseGroupRepository.existsById_UserIdAndId_DiseaseGroupId(userId, diseaseGroupId)) {
            throw new ForbiddenException("Bu hastalık grubuna üye değilsiniz, yorum yapamazsınız");
        }
    }

    // Yanıt verilen yorum (üst-seviye ya da başka bir yanıt olabilir) aynı
    // gönderiye ait olmalı. Derinlik sınırsızdır - Twitter/Reddit tarzı
    // yanıta yanıt zincirleri desteklenir.
    private Comment resolveParent(Long parentCommentId, Long postId) {
        if (parentCommentId == null) {
            return null;
        }
        Comment parent = getById(parentCommentId);
        if (!parent.getPost().getId().equals(postId)) {
            throw new BadRequestException("Yanıt verilen yorum bu gönderiye ait değil");
        }
        return parent;
    }

    // Rapor: küfür/spam içeriyorsa (blocked) BadRequestException fırlatılır;
    // kriz sinyali taşıyorsa (bloklamadan) true döner (bkz.
    // ContentModerationService javadoc'u / PostServiceImpl.moderateOrThrow
    // ile aynı desen).
    private boolean moderateOrThrow(String content) {
        ContentModerationService.ModerationResult result = contentModerationService.moderate(content);
        if (result.blocked()) {
            throw new BadRequestException(result.blockReason());
        }
        return result.sensitive();
    }
}
