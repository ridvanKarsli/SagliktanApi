package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.ReactionTargetType;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;

import java.util.Collection;
import java.util.Map;

public interface ReactionService {

    // Kullanıcının hedefe verdiği reaksiyonu oluşturur ya da (zaten varsa)
    // günceller - upsert.
    void setReaction(ReactionTargetType targetType, Long targetId, Long userId, ReactionValue value);

    // Idempotent: reaksiyon yoksa sessizce hiçbir şey yapmaz.
    void removeReaction(ReactionTargetType targetType, Long targetId, Long userId);

    ReactionSummary getSummary(ReactionTargetType targetType, Long targetId, Long userId);

    // Liste ekranları için toplu özet - N+1'i önler.
    Map<Long, ReactionSummary> getSummaries(ReactionTargetType targetType, Collection<Long> targetIds, Long userId);
}
