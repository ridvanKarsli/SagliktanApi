package com.ridvankarsli.sagliktanapi.repository;

import com.ridvankarsli.sagliktanapi.domain.Reaction;
import com.ridvankarsli.sagliktanapi.domain.ReactionTargetType;
import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByTargetTypeAndTargetIdAndUserId(ReactionTargetType targetType, Long targetId, Long userId);

    void deleteByTargetTypeAndTargetIdAndUserId(ReactionTargetType targetType, Long targetId, Long userId);

    // Bir kullanıcının, birden fazla hedef (ör. bir sayfadaki tüm postlar)
    // için kendi reaksiyonlarını tek sorguda çekmek için - N+1'i önler.
    List<Reaction> findByTargetTypeAndTargetIdInAndUserId(
            ReactionTargetType targetType, Collection<Long> targetIds, Long userId);

    // Birden fazla hedefin HELPFUL/NOT_HELPFUL sayaçlarını tek sorguda,
    // gruplanmış halde döner - liste ekranlarında (post feed, yorum ağacı)
    // her satır için ayrı count sorgusu atmamak için.
    @Query("select r.targetId as targetId, r.value as value, count(r) as count "
            + "from Reaction r where r.targetType = :targetType and r.targetId in :targetIds "
            + "group by r.targetId, r.value")
    List<ReactionCountRow> countGrouped(
            @Param("targetType") ReactionTargetType targetType, @Param("targetIds") Collection<Long> targetIds);

    interface ReactionCountRow {
        Long getTargetId();
        ReactionValue getValue();
        long getCount();
    }
}
