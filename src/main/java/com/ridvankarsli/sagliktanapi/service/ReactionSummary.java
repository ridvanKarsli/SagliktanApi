package com.ridvankarsli.sagliktanapi.service;

import com.ridvankarsli.sagliktanapi.domain.ReactionValue;

// Bir içeriğin (post/yorum) reaksiyon özeti: toplam sayaçlar + varsa
// isteği yapan kullanıcının kendi reaksiyonu (myReaction null ise hiç
// reaksiyon vermemiş demektir).
public record ReactionSummary(long helpfulCount, long notHelpfulCount, ReactionValue myReaction) {
    public static ReactionSummary empty() {
        return new ReactionSummary(0, 0, null);
    }
}
