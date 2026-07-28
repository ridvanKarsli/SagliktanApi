package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.Reaction;
import com.ridvankarsli.sagliktanapi.domain.ReactionTargetType;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.ReactionRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.ReactionService;
import com.ridvankarsli.sagliktanapi.service.ReactionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void setReaction(ReactionTargetType targetType, Long targetId, Long userId, ReactionValue value) {
        assertTargetExists(targetType, targetId);

        Reaction reaction = reactionRepository
                .findByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId)
                .orElseGet(() -> Reaction.builder()
                        .targetType(targetType)
                        .targetId(targetId)
                        // getReferenceById: kullanıcıyı tekrar SELECT ile çekmeden
                        // (zaten @AuthenticationPrincipal'dan geliyor, geçerliliği
                        // biliniyor) sadece foreign key ilişkisi için proxy alır.
                        .user(userRepository.getReferenceById(userId))
                        .build());

        reaction.setValue(value);
        reactionRepository.save(reaction);
    }

    @Override
    @Transactional
    public void removeReaction(ReactionTargetType targetType, Long targetId, Long userId) {
        reactionRepository.deleteByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
    }

    @Override
    public ReactionSummary getSummary(ReactionTargetType targetType, Long targetId, Long userId) {
        return getSummaries(targetType, java.util.List.of(targetId), userId)
                .getOrDefault(targetId, ReactionSummary.empty());
    }

    @Override
    public Map<Long, ReactionSummary> getSummaries(ReactionTargetType targetType, Collection<Long> targetIds, Long userId) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, long[]> counts = new HashMap<>();
        for (Long id : targetIds) {
            counts.put(id, new long[]{0L, 0L});
        }

        for (ReactionRepository.ReactionCountRow row : reactionRepository.countGrouped(targetType, targetIds)) {
            long[] slot = counts.get(row.getTargetId());
            if (slot == null) {
                continue;
            }
            if (row.getValue() == ReactionValue.HELPFUL) {
                slot[0] = row.getCount();
            } else {
                slot[1] = row.getCount();
            }
        }

        Map<Long, ReactionValue> mine = new HashMap<>();
        if (userId != null) {
            reactionRepository.findByTargetTypeAndTargetIdInAndUserId(targetType, targetIds, userId)
                    .forEach(r -> mine.put(r.getTargetId(), r.getValue()));
        }

        Map<Long, ReactionSummary> result = new HashMap<>();
        for (Long id : targetIds) {
            long[] slot = counts.get(id);
            result.put(id, new ReactionSummary(slot[0], slot[1], mine.get(id)));
        }
        return result;
    }

    private void assertTargetExists(ReactionTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(
                    targetType == ReactionTargetType.POST ? "Gönderi bulunamadı" : "Yorum bulunamadı");
        }
    }
}
